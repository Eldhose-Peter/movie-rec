package com.example.recommendation.batch.step4;

import com.example.recommendation.model.RatingEvent;
import com.example.recommendation.model.UserSimilarity;
import com.example.recommendation.model.UserSimilarityKey;
import com.example.recommendation.repository.RatingRepository;
import com.example.recommendation.service.lsh.Similarity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class UserSimilarityProcessor {
    @Bean
    @StepScope
    public ItemProcessor<UserSimilarityKey, UserSimilarity> similarityProcessor(RatingRepository ratingRepo) {
        return candidate -> {
            long start = System.currentTimeMillis();
            int rater1 = candidate.getRaterId();
            int rater2 = candidate.getOtherRaterId();

            // --- Step 1: Fetch ratings ---
            long fetchStart = System.currentTimeMillis();
            List<RatingEvent> ratings1 = ratingRepo.findById_RaterId(rater1);
            List<RatingEvent> ratings2 = ratingRepo.findById_RaterId(rater2);
            long fetchEnd = System.currentTimeMillis();

            // --- Step 2: Build maps ---
            Map<Integer, Double> r1 = new HashMap<>();
            for (RatingEvent row : ratings1) {
                r1.put(row.getMovieId(), row.getRating());
            }
            Map<Integer, Double> r2 = new HashMap<>();
            for (RatingEvent row : ratings2) {
                r2.put(row.getMovieId(), row.getRating());
            }
            long mapEnd = System.currentTimeMillis();

            // --- Step 3: Compute similarity ---
            double sim = Similarity.cosine(r1, r2);
            long simEnd = System.currentTimeMillis();

            // --- Step 4: Log timings ---
            long totalTime = simEnd - start;
            log.debug(
                    "Processed pair ({}, {}) | fetch={} ms | map={} ms | sim={} ms | total={} ms | ratings1={} | ratings2={}",
                    rater1, rater2,
                    (fetchEnd - fetchStart),
                    (mapEnd - fetchEnd),
                    (simEnd - mapEnd),
                    totalTime,
                    ratings1.size(),
                    ratings2.size()
            );

            return new UserSimilarity(rater1, rater2, sim);
        };
    }

    @Bean
    @StepScope
    public ItemProcessor<UserSimilarityKey, UserSimilarity> similarityInMemoryProcessor(RatingRepository ratingRepo) {
        // Preload all ratings into memory at step start
        Map<Integer, Map<Integer, Double>> userRatingsCache = ratingRepo.findAll()
                .stream()
                .collect(Collectors.groupingBy(
                        r -> r.getId().getRaterId(),
                        Collectors.toMap(
                                RatingEvent::getMovieId,
                                RatingEvent::getRating
                        )
                ));
        log.info("Preloaded all ratings into memory at step start");

        return candidate -> {
            Map<Integer, Double> r1 = userRatingsCache.getOrDefault(candidate.getRaterId(), Map.of());
            Map<Integer, Double> r2 = userRatingsCache.getOrDefault(candidate.getOtherRaterId(), Map.of());

            // Compute similarity
            double sim = Similarity.cosine(r1, r2);

            return new UserSimilarity(candidate.getRaterId(), candidate.getOtherRaterId(), sim);
        };
    }




}
