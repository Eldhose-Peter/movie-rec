package com.example.recommendation.service.strategy;

import com.example.recommendation.model.ImdbRatingEvent;
import com.example.recommendation.model.SimilarItem;
import com.example.recommendation.repository.RatingRepository;
import com.example.recommendation.service.similarity.RatingNormalizer;
import com.example.recommendation.service.similarity.SimilarityCalculator;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Strategy that loads all ratings into memory in a single query.
 * Groups by rater and computes similarities in parallel.
 *
 * Pros:
 * - Single database query (efficient from query perspective)
 * - All data available for processing
 *
 * Cons:
 * - High memory usage: ~650 MB for 13M rating events (rough estimate)
 * - Parallel streams create additional copies in each thread
 * - Large result set can overwhelm network bandwidth and DB driver
 * - Slow if dataset is very large
 */
@Slf4j
public class LoadAllStrategy implements RaterSimilarityStrategy {

        @Override
        public List<SimilarItem> compute(Integer currentRaterId,
                        Map<Integer, Double> currentRatingsMap,
                        RatingRepository repository) {
                log.info("Starting LoadAllStrategy for rater {}", currentRaterId);

                // Load all ratings from database in a single query
                List<ImdbRatingEvent> allRatings = repository.findAll();
                log.debug("Loaded {} total rating events from DB", allRatings.size());

                // Group ratings by rater ID
                Map<Integer, List<ImdbRatingEvent>> ratingsByRater = allRatings.stream()
                                .collect(Collectors.groupingBy(ImdbRatingEvent::getRaterId));

                log.debug("Grouped ratings by {} raters", ratingsByRater.size());

                // Compute similarities for all other raters in parallel
                List<SimilarItem> similarities = ratingsByRater.entrySet()
                                .parallelStream()
                                .filter(entry -> !entry.getKey().equals(currentRaterId))
                                .map(entry -> {
                                        Integer otherRaterId = entry.getKey();
                                        Map<Integer, Double> otherRatingsMap = RatingNormalizer.normalize(
                                                        entry.getValue());

                                        // Compute dot product
                                        double similarity = SimilarityCalculator.computeDotProduct(
                                                        currentRatingsMap, otherRatingsMap);

                                        return new SimilarItem(otherRaterId, similarity);
                                })
                                .sorted(Comparator.comparing(SimilarItem::getSimilarity).reversed())
                                .collect(Collectors.toList());

                log.info("LoadAllStrategy computed {} similarities", similarities.size());
                return similarities;
        }
}
