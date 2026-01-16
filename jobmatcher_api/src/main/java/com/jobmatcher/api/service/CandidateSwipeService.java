package com.jobmatcher.api.service;

import com.jobmatcher.api.domain.job.Job;
import com.jobmatcher.api.domain.job.JobStatus;
import com.jobmatcher.api.domain.job.JobSwipe;
import com.jobmatcher.api.domain.job.SwipeAction;
import com.jobmatcher.api.dto.*;
import com.jobmatcher.api.exception.BadRequestException;
import com.jobmatcher.api.exception.NotFoundException;
import com.jobmatcher.api.repository.JobRepository;
import com.jobmatcher.api.repository.JobSwipeRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CandidateSwipeService {

    private final JobRepository jobRepo;
    private final JobSwipeRepository swipeRepo;
    private final JobService jobService; // per riusare toDto()

    public CandidateSwipeService(JobRepository jobRepo, JobSwipeRepository swipeRepo, JobService jobService) {
        this.jobRepo = jobRepo;
        this.swipeRepo = swipeRepo;
        this.jobService = jobService;
    }

    private String currentUsername() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    public List<SwipeFeedItemDTO> getSwipeFeed(Double lat, Double lon, Double radiusKm, Integer limit) {
        String candidate = currentUsername();

        int lim = (limit == null) ? 20 : Math.max(1, Math.min(limit, 100));
        double r = (radiusKm == null) ? 25.0 : Math.max(1.0, Math.min(radiusKm, 500.0));

        // 1) Prendo tutti i PUBLISHED (senza paginazione: ok per demo)
        List<Job> jobs = jobRepo.findByStatusOrderByCreatedAtDesc(JobStatus.PUBLISHED);

        // 2) Escludo i job già swipati
        Set<UUID> alreadySeen = swipeRepo.findByCandidateUsernameOrderByCreatedAtDesc(candidate).stream()
                .map(JobSwipe::getJobId)
                .collect(Collectors.toSet());

        // 3) Filtro + calcolo distanza
        List<SwipeFeedItemDTO> items = new ArrayList<>();
        for (Job j : jobs) {
            if (alreadySeen.contains(j.getId())) continue;

            Double d = distanceKm(lat, lon, j.getLat(), j.getLon());
            if (d != null && d > r) continue;

            // score demo: per ora solo proximityScore (0..1)
            double score = proximityScore(d, r);

            items.add(new SwipeFeedItemDTO(jobService.toDto(j), d, score));
        }

        // 4) Ordino: score desc, poi createdAt desc
        items.sort(Comparator
                .comparing((SwipeFeedItemDTO it) -> it.score() != null ? it.score() : 0.0).reversed());

        return items.size() <= lim ? items : items.subList(0, lim);
    }

    public SwipeResponse swipe(SwipeRequest req) {
        if (req == null || req.jobId() == null || req.action() == null || req.action().trim().isEmpty()) {
            throw new BadRequestException("jobId e action sono obbligatori");
        }

        SwipeAction action;
        try {
            action = SwipeAction.valueOf(req.action().trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new BadRequestException("action non valida (LIKE, DISLIKE)");
        }

        Job job = jobRepo.findById(req.jobId()).orElseThrow(() -> new NotFoundException("Job non trovato"));
        if (job.getStatus() != JobStatus.PUBLISHED) {
            throw new NotFoundException("Job non trovato");
        }

        String candidate = currentUsername();

        JobSwipe swipe = swipeRepo.findByCandidateUsernameAndJobId(candidate, req.jobId())
                .orElseGet(() -> JobSwipe.builder()
                        .candidateUsername(candidate)
                        .jobId(req.jobId())
                        .build());

        swipe.setAction(action);
        JobSwipe saved = swipeRepo.save(swipe);

        return new SwipeResponse(saved.getJobId(), saved.getAction().name(), saved.getCreatedAt());
    }

    // --- helpers: haversine ---
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
        if (distance == null) return 0.5; // se manca geo, score neutro
        double x = Math.max(0.0, Math.min(distance / radiusKm, 1.0));
        return 1.0 - x;
    }
}
