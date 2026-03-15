package com.example.recommendation.service.strategy;

import com.example.recommendation.model.SimilarItem;
import com.example.recommendation.repository.RatingRepository;
import com.example.recommendation.service.similarity.RatingNormalizer;
import com.example.recommendation.service.similarity.SimilarityCalculator;
import lombok.extern.slf4j.Slf4j;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Strategy that fetches ratings for each rater individually from the database.
 * Uses parallelStream to compute similarities concurrently.
 *
 * Pros:
 * - Straightforward approach
 *
 * Cons:
 * - High DB load: 138k+ individual queries (one per rater)
 * - Network overhead: Many round trips to database
 * - Latency: With 138k raters × 10ms per query ≈ 23 minutes
 * - Memory: Parallel threads each load ratings maps
 */
@Slf4j
public class MultiQueryStrategy implements RaterSimilarityStrategy {

        @Override
        public List<SimilarItem> compute(Integer currentRaterId,
                        Map<Integer, Double> currentRatingsMap,
                        RatingRepository repository) {
                log.info("Starting MultiQueryStrategy for rater {}", currentRaterId);

                // Get all rater IDs from database
                List<Integer> raterIds = repository.getUniqueRaterIds();
                log.debug("Fetched {} unique rater IDs from DB", raterIds.size());

                // Compute similarities in parallel for all other raters
                List<SimilarItem> similarities = raterIds
                                .parallelStream()
                                .filter(otherRaterId -> !Objects.equals(otherRaterId, currentRaterId))
                                .map(otherRaterId -> {
                                        // Fetch other rater's ratings from DB
                                        Map<Integer, Double> otherRatingsMap = RatingNormalizer.normalize(
                                                        repository.findById_RaterId(otherRaterId));

                                        // Compute dot product
                                        double similarity = SimilarityCalculator.computeDotProduct(
                                                        currentRatingsMap, otherRatingsMap);

                                        return new SimilarItem(otherRaterId, similarity);
                                })
                                .sorted(Comparator.comparing(SimilarItem::getSimilarity).reversed())
                                .collect(Collectors.toList());

                log.info("MultiQueryStrategy computed {} similarities", similarities.size());
                return similarities;
        }
}
