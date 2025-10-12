package com.example.recommendation.batch.step4;

import com.example.recommendation.model.RatingEvent;
import com.example.recommendation.model.UserSimilarity;
import com.example.recommendation.model.UserSimilarityKey;
import com.example.recommendation.repository.RatingRepository;
import com.example.recommendation.service.lsh.Similarity;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class UserSimilarityProcessor {
    @Bean
    @StepScope
    public ItemProcessor<UserSimilarityKey, UserSimilarity> similarityProcessor(RatingRepository ratingRepo) {
        return candidate -> {
            List<RatingEvent> ratings1 = ratingRepo.findById_RaterId(candidate.getRaterId());
            Map<Integer, Double> r1 = new HashMap<>();
            for (RatingEvent row : ratings1) {
                r1.put(row.getMovieId(), row.getRating());
            }

            List<RatingEvent> ratings2 = ratingRepo.findById_RaterId(candidate.getOtherRaterId());
            Map<Integer, Double> r2 = new HashMap<>();
            for (RatingEvent row : ratings2) {
                r2.put(row.getMovieId(), row.getRating());
            }

            double sim = Similarity.cosine(r1, r2);
            return new UserSimilarity(candidate.getRaterId(), candidate.getOtherRaterId(), sim);
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

        return candidate -> {
            Map<Integer, Double> r1 = userRatingsCache.getOrDefault(candidate.getRaterId(), Map.of());
            Map<Integer, Double> r2 = userRatingsCache.getOrDefault(candidate.getOtherRaterId(), Map.of());

            // Compute similarity
            double sim = Similarity.cosine(r1, r2);

            return new UserSimilarity(candidate.getRaterId(), candidate.getOtherRaterId(), sim);
        };
    }


}
