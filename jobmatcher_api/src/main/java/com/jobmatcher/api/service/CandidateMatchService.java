package com.jobmatcher.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobmatcher.api.domain.curriculum.CvFile;
import com.jobmatcher.api.domain.curriculum.CvProcessingStatus;
import com.jobmatcher.api.domain.job.Job;
import com.jobmatcher.api.domain.job.JobStatus;
import com.jobmatcher.api.domain.job.JobSwipe;
import com.jobmatcher.api.domain.job.SwipeAction;
import com.jobmatcher.api.dto.MatchItemDTO;
import com.jobmatcher.api.dto.JobDTO;
import com.jobmatcher.api.exception.BadRequestException;
import com.jobmatcher.api.repository.CvFileRepository;
import com.jobmatcher.api.repository.JobRepository;
import com.jobmatcher.api.repository.JobSwipeRepository;
import com.jobmatcher.api.domain.candidate.CandidateProfile;
import com.jobmatcher.api.repository.CandidateProfileRepository;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CandidateMatchService {

    private final JobRepository jobRepo;
    private final JobSwipeRepository swipeRepo;
    private final CvFileRepository cvRepo;
    private final CandidateProfileRepository profileRepo;
    private final JobService jobService; // per DTO mapper

    public CandidateMatchService(JobRepository jobRepo, JobSwipeRepository swipeRepo, CvFileRepository cvRepo, CandidateProfileRepository profileRepo, JobService jobService) {
        this.jobRepo = jobRepo;
        this.swipeRepo = swipeRepo;
        this.cvRepo = cvRepo;
        this.profileRepo = profileRepo;
        this.jobService = jobService;
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public List<MatchItemDTO> getMatches(Double lat, Double lon, Double radiusKm, Integer limit) {
        String candidate = currentUsername();

        int lim = (limit == null) ? 20 : Math.max(1, Math.min(limit, 100));
        double r = (radiusKm == null) ? 25.0 : Math.max(1.0, Math.min(radiusKm, 500.0));

        // 1) CV PARSED (obbligatorio) — usa active_cv_file_id se presente
        CvFile cv = resolveActiveParsedCv(candidate);

        // testo/token CV (fallback)
        String cvText = extractCvText(cv.getAnalysisJson());
        Set<String> cvTokens = tokenize(cvText);

        // embedding CV (preferred)
        double[] cvEmb = extractEmbeddingFromCv(cv.getAnalysisJson());

        // 2) Job likati (solo LIKE)
        List<UUID> likedJobIds = swipeRepo
                .findByCandidateUsernameAndActionOrderByCreatedAtDesc(candidate, SwipeAction.LIKE)
                .stream()
                .map(JobSwipe::getJobId)
                .toList();

        if (likedJobIds.isEmpty()) return List.of();

        // 3) Carico job e filtro PUBLISHED
        Map<UUID, Job> jobsById = jobRepo.findAllById(likedJobIds).stream()
                .filter(j -> j.getStatus() == JobStatus.PUBLISHED)
                .collect(Collectors.toMap(Job::getId, j -> j));

        List<MatchItemDTO> items = new ArrayList<>();

        for (UUID id : likedJobIds) {
            Job j = jobsById.get(id);
            if (j == null) continue;

            // filtro raggio (se geo disponibile)
            Double d = distanceKm(lat, lon, j.getLat(), j.getLon());
            if (d != null && d > r) continue;

            double proximity = proximityScore(d, r); // 0..1

            // --- text score: embedding se possibile, altrimenti keyword overlap ---
            double textScore;
            boolean usedEmbedding = false;

            double[] jobEmb = extractEmbeddingFromJob(j.getEmbedding());
            if (cvEmb != null && jobEmb != null && cvEmb.length == jobEmb.length) {
                textScore = cosine(cvEmb, jobEmb);
                usedEmbedding = true;
            } else {
                String jobText = buildJobText(j);
                Set<String> jobTokens = tokenize(jobText);
                textScore = jaccard(cvTokens, jobTokens);
            }

            // pesi
            double score = 0.35 * proximity + 0.65 * textScore;

            // reasons
            List<String> reasons;
            if (usedEmbedding) {
                // puoi tenere reasons "semantiche" + qualche overlap (se vuoi)
                String jobText = buildJobText(j);
                Set<String> jobTokens = tokenize(jobText);
                List<String> overlap = topOverlap(cvTokens, jobTokens, 4);
                reasons = new ArrayList<>();
                reasons.add("vector_similarity");
                reasons.addAll(overlap);
            } else {
                String jobText = buildJobText(j);
                Set<String> jobTokens = tokenize(jobText);
                reasons = topOverlap(cvTokens, jobTokens, 5);
            }

            JobDTO dto = jobService.toDto(j);
            items.add(new MatchItemDTO(dto, d, round(score, 4), reasons));
        }

        items.sort(Comparator.comparing((MatchItemDTO it) -> it.score() != null ? it.score() : 0.0).reversed());
        return items.size() <= lim ? items : items.subList(0, lim);
    }

    private CvFile resolveActiveParsedCv(String candidate) {
        UUID activeId = profileRepo.findByOwnerUsername(candidate)
                .map(CandidateProfile::getActiveCvFileId)   // assicurati che l’entity abbia getter
                .orElse(null);

        if (activeId != null) {
            return cvRepo.findByIdAndOwnerUsernameAndStatus(activeId, candidate, CvProcessingStatus.PARSED)
                    .orElseThrow(() -> new BadRequestException(
                            "Il CV attivo non è disponibile o non è stato analizzato. Carica/analizza un CV."
                    ));
        }

        // fallback: se non è settato l’attivo (es. utenti vecchi), usa l’ultimo PARSED
        return cvRepo.findFirstByOwnerUsernameAndStatusOrderByUploadedAtDesc(candidate, CvProcessingStatus.PARSED)
                .orElseThrow(() -> new BadRequestException("Carica e analizza un CV prima di vedere i match"));
    }



    private String extractCvText(JsonNode analysisJson) {
        if (analysisJson == null) return "";
        JsonNode t = analysisJson.get("text");
        return (t != null && t.isTextual()) ? t.asText("") : "";
    }

    private double[] extractEmbeddingFromCv(JsonNode analysisJson) {
        if (analysisJson == null) return null;
        JsonNode emb = analysisJson.get("embedding");
        if (emb == null || !emb.isArray() || emb.size() == 0) return null;

        double[] v = new double[emb.size()];
        for (int i = 0; i < emb.size(); i++) {
            JsonNode n = emb.get(i);
            if (n == null || !n.isNumber()) return null;
            v[i] = n.asDouble();
        }
        return v;
    }

    private String buildJobText(Job j) {
        StringBuilder sb = new StringBuilder();
        if (j.getTitle() != null) sb.append(j.getTitle()).append(" ");
        if (j.getDescription() != null) sb.append(j.getDescription()).append(" ");
        if (j.getContractType() != null) sb.append(j.getContractType()).append(" ");
        if (j.getSeniority() != null) sb.append(j.getSeniority()).append(" ");
        if (j.getLocation() != null) sb.append(j.getLocation()).append(" ");
        return sb.toString();
    }

    private double[] extractEmbeddingFromJob(JsonNode emb) {
        if (emb == null || emb.isNull() || !emb.isArray() || emb.size() == 0) return null;

        double[] v = new double[emb.size()];
        for (int i = 0; i < emb.size(); i++) {
            JsonNode n = emb.get(i);
            if (n == null || !n.isNumber()) return null;
            v[i] = n.asDouble();
        }
        return v;
    }


    private Set<String> tokenize(String s) {
        if (s == null) return Set.of();
        String[] parts = s.toLowerCase(Locale.ROOT).split("[^a-z0-9àèéìòóùüöä#\\+]+");
        Set<String> out = new HashSet<>();
        for (String p : parts) {
            if (p == null) continue;
            String t = p.trim();
            if (t.length() < 3) continue;
            out.add(t);
        }
        return out;
    }

    private double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() || b.isEmpty()) return 0.0;
        int inter = 0;
        for (String x : a) if (b.contains(x)) inter++;
        int union = a.size() + b.size() - inter;
        return union == 0 ? 0.0 : (double) inter / (double) union;
    }

    private List<String> topOverlap(Set<String> a, Set<String> b, int k) {
        List<String> inter = a.stream().filter(b::contains).sorted().toList();
        return inter.size() <= k ? inter : inter.subList(0, k);
    }

    private double cosine(double[] a, double[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length) return 0.0;

        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0.0 || nb == 0.0) return 0.0;
        double cos = dot / (Math.sqrt(na) * Math.sqrt(nb));

        // clamp + normalizza in [0..1] per usarlo come score
        cos = Math.max(-1.0, Math.min(1.0, cos));
        return (cos + 1.0) / 2.0;
    }


    private Double distanceKm(Double lat1, Double lon1, Double lat2, Double lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return null;
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double proximityScore(Double distance, double radiusKm) {
        if (distance == null) return 0.5; // neutro se geo mancante
        double x = Math.max(0.0, Math.min(distance / radiusKm, 1.0));
        return 1.0 - x;
    }

    private Double round(double v, int decimals) {
        double p = Math.pow(10, decimals);
        return Math.round(v * p) / p;
    }
}
