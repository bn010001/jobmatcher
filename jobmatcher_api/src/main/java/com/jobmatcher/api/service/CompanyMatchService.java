package com.jobmatcher.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.jobmatcher.api.domain.curriculum.CvFile;
import com.jobmatcher.api.domain.curriculum.CvProcessingStatus;
import com.jobmatcher.api.domain.job.*;
import com.jobmatcher.api.dto.CompanyMatchItemDTO;
import com.jobmatcher.api.exception.BadRequestException;
import com.jobmatcher.api.repository.CvFileRepository;
import com.jobmatcher.api.repository.JobRepository;
import com.jobmatcher.api.repository.JobSwipeRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CompanyMatchService {

    private final JobRepository jobRepo;
    private final JobSwipeRepository swipeRepo;
    private final CvFileRepository cvRepo;

    public CompanyMatchService(JobRepository jobRepo, JobSwipeRepository swipeRepo, CvFileRepository cvRepo) {
        this.jobRepo = jobRepo;
        this.swipeRepo = swipeRepo;
        this.cvRepo = cvRepo;
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public List<CompanyMatchItemDTO> getCompanyMatches(Integer limit) {
        String company = currentUsername();
        int lim = (limit == null) ? 50 : Math.max(1, Math.min(limit, 200));

        // 1) solo job PUBLISHED della company
        List<Job> myJobs = jobRepo.findByOwnerUsernameAndStatusOrderByCreatedAtDesc(company, JobStatus.PUBLISHED);
        if (myJobs.isEmpty()) return List.of();

        List<UUID> jobIds = myJobs.stream().map(Job::getId).toList();
        Map<UUID, Job> jobById = myJobs.stream().collect(Collectors.toMap(Job::getId, j -> j));

        // 2) like ricevuti sui miei job
        List<JobSwipe> likes = swipeRepo.findByJobIdInAndActionOrderByCreatedAtDesc(jobIds, SwipeAction.LIKE);
        if (likes.isEmpty()) return List.of();

        List<CompanyMatchItemDTO> out = new ArrayList<>();

        for (JobSwipe like : likes) {
            Job job = jobById.get(like.getJobId());
            if (job == null) continue;

            String candidate = like.getCandidateUsername();

            // 3) CV del candidato (parsed) per score “smart”
            Optional<CvFile> cvOpt = cvRepo.findFirstByOwnerUsernameAndStatusOrderByUploadedAtDesc(candidate, CvProcessingStatus.PARSED);
            if (cvOpt.isEmpty()) {
                // per demo: se non ha CV parsato, score neutro
                out.add(new CompanyMatchItemDTO(
                        job.getId(),
                        job.getTitle(),
                        candidate,
                        0.10,
                        List.of("cv_non_disponibile"),
                        like.getCreatedAt()
                ));
                continue;
            }

            CvFile cv = cvOpt.get();
            double[] cvEmb = extractEmbeddingFromCv(cv.getAnalysisJson());
            double[] jobEmb = extractEmbeddingFromJob(job.getEmbedding());

            // Similarità AI (cosine) se entrambi disponibili
            Double embScore = null;
            if (cvEmb != null && jobEmb != null && cvEmb.length == jobEmb.length && cvEmb.length > 0) {
                embScore = cosine(cvEmb, jobEmb); // -1..1
                embScore = (embScore + 1.0) / 2.0; // 0..1
            }

            // Fallback / reasons: overlap keyword su testo
            String cvText = extractText(cv.getAnalysisJson());
            Set<String> cvTokens = tokenize(cvText);
            Set<String> jobTokens = tokenize(buildJobText(job));
            double keywordScore = jaccard(cvTokens, jobTokens);
            List<String> reasons = topOverlap(cvTokens, jobTokens, 5);

            double score;
            if (embScore != null) {
                score = 0.75 * embScore + 0.25 * keywordScore;
            } else {
                score = keywordScore;
            }

            out.add(new CompanyMatchItemDTO(
                    job.getId(),
                    job.getTitle(),
                    candidate,
                    round(score, 4),
                    reasons.isEmpty() ? List.of("match_testuale") : reasons,
                    like.getCreatedAt()
            ));
        }

        // ordina per score desc, poi likedAt desc (già quasi così)
        out.sort(Comparator
                .comparing((CompanyMatchItemDTO it) -> it.score() != null ? it.score() : 0.0).reversed()
                .thenComparing((CompanyMatchItemDTO it) -> it.likedAt() != null ? it.likedAt() : java.time.Instant.EPOCH).reversed()
        );

        return out.size() <= lim ? out : out.subList(0, lim);
    }

    // ---------------- helpers ----------------

    private String extractText(JsonNode analysisJson) {
        if (analysisJson == null) return "";
        JsonNode t = analysisJson.get("text");
        return (t != null && t.isTextual()) ? t.asText("") : "";
    }

    private double[] extractEmbeddingFromCv(JsonNode analysisJson) {
        if (analysisJson == null) return null;
        JsonNode emb = analysisJson.get("embedding");
        return jsonArrayToDoubleArray(emb);
    }

    private double[] extractEmbeddingFromJob(JsonNode jobEmbeddingJson) {
        return jsonArrayToDoubleArray(jobEmbeddingJson);
    }

    private double[] jsonArrayToDoubleArray(JsonNode node) {
        if (node == null || !node.isArray()) return null;
        int n = node.size();
        double[] v = new double[n];
        for (int i = 0; i < n; i++) v[i] = node.get(i).asDouble();
        return v;
    }

    private double cosine(double[] a, double[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
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

    private Double round(double v, int decimals) {
        double p = Math.pow(10, decimals);
        return Math.round(v * p) / p;
    }
}

