package com.example.recommendation.batch.step4;

import com.example.recommendation.model.RatingEvent;
import com.example.recommendation.model.UserSimilarity;
import com.example.recommendation.model.UserSimilarityKey;
import com.example.recommendation.repository.RatingRepository;
import com.example.recommendation.service.lsh.Similarity;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.time.Duration;
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

    @Bean
    @StepScope
    public ItemProcessor<UserSimilarityKey, UserSimilarity> similarityLRUCacheProcessor(RatingRepository ratingRepo) {

        Cache<Integer, List<RatingEvent>> ratingCache = Caffeine.newBuilder()
                .maximumSize(10_000)               // keep 10k raters in memory
                .expireAfterAccess(Duration.ofMinutes(20))
                .recordStats()
                .build();

        return candidate -> {
            long start = System.nanoTime();

            int r1Id = candidate.getRaterId();
            int r2Id = candidate.getOtherRaterId();

            // cached or loaded on demand
            List<RatingEvent> ratings1 = ratingCache.get(r1Id, ratingRepo::findById_RaterId);
            List<RatingEvent> ratings2 = ratingCache.get(r2Id, ratingRepo::findById_RaterId);

            Map<Integer, Double> r1 = new HashMap<>();
            for (RatingEvent r : ratings1) r1.put(r.getMovieId(), r.getRating());

            Map<Integer, Double> r2 = new HashMap<>();
            for (RatingEvent r : ratings2) r2.put(r.getMovieId(), r.getRating());

            double sim = Similarity.cosine(r1, r2);

            long end = System.nanoTime();
            log.debug("Processed pair ({}, {}) | cacheSize={} | total={} ms",
                    r1Id, r2Id, ratingCache.estimatedSize(), (end - start) / 1_000_000.0);

            return new UserSimilarity(r1Id, r2Id, sim);
        };
    }

    @Bean
    @StepScope
    public ItemProcessor<UserSimilarityKey, UserSimilarity> similarityPrefetchProcessor(RatingRepository ratingRepo, RatingsPrefetchListener prefetchListener) {
        return candidate -> {
            int r1Id = candidate.getRaterId();
            int r2Id = candidate.getOtherRaterId();

            // cached or loaded on demand
            Map<Integer, List<RatingEvent>> ratingsCache = prefetchListener.getRatingsCache();
            List<RatingEvent> ratings1 = ratingsCache.getOrDefault(r1Id, List.of());
            List<RatingEvent> ratings2 = ratingsCache.getOrDefault(r2Id, List.of());


            Map<Integer, Double> r1 = new HashMap<>();
            for (RatingEvent r : ratings1) r1.put(r.getMovieId(), r.getRating());

            Map<Integer, Double> r2 = new HashMap<>();
            for (RatingEvent r : ratings2) r2.put(r.getMovieId(), r.getRating());

            double sim = Similarity.dotProduct(r1, r2);

            if (sim <= 0) {
                return null;
            }

            return new UserSimilarity(r1Id, r2Id, sim);
        };
    }





}
