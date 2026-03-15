package com.example.recommendation.service.similarity;

import com.example.recommendation.config.SimilarityProperties;
import com.example.recommendation.config.SimilarityStrategyFactory;
import com.example.recommendation.model.ImdbRatingEvent;
import com.example.recommendation.model.SimilarItem;
import com.example.recommendation.repository.RatingRepository;
import com.example.recommendation.service.strategy.RaterSimilarityStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Service for computing rater-to-rater similarity.
 * Uses the Strategy Pattern to support different data fetching approaches.
 *
 * Strategies available:
 * - multi_query: Per-rater DB queries
 * - load_all: Load all ratings in memory at once
 * - stream_batch_count: Stream with batch by count
 * - stream_batch_rater: Stream with batch by rater (DEFAULT)
 * - offset_limit: OFFSET/LIMIT pagination
 * - lsh_approximate: LSH-based approximate similarity
 *
 * Configure via: similarity.strategy property in application.properties
 */
@Slf4j
@Service
public class RaterSimilarityService {

    private final RatingRepository ratingRepository;
    private final SimilarityStrategyFactory strategyFactory;
    private final SimilarityProperties similarityProperties;

    public RaterSimilarityService(RatingRepository ratingRepository,
            SimilarityStrategyFactory strategyFactory,
            SimilarityProperties similarityProperties) {
        this.ratingRepository = ratingRepository;
        this.strategyFactory = strategyFactory;
        this.similarityProperties = similarityProperties;
        log.info("RaterSimilarityService initialized with strategy: {}",
                similarityProperties.getStrategy());
    }

    /**
     * Get all similar raters for a given rater.
     * Returns similarities sorted by score (highest first).
     *
     * @param currentRaterId the rater to find similarities for
     * @return list of similar items (rater IDs with similarity scores)
     */
    public List<SimilarItem> getSimilarRaters(Integer currentRaterId) {
        if (currentRaterId == null) {
            log.warn("currentRaterId is null");
            return Collections.emptyList();
        }

        log.info("Computing similar raters for rater {} using strategy: {}",
                currentRaterId, similarityProperties.getStrategy());

        // Load current rater's ratings
        List<ImdbRatingEvent> currentRatings = ratingRepository
                .findById_RaterId(currentRaterId);
        if (currentRatings.isEmpty()) {
            log.warn("No ratings found for rater {}", currentRaterId);
            return Collections.emptyList();
        }

        // Normalize ratings
        Map<Integer, Double> normalizedRatings = RatingNormalizer.normalize(currentRatings);

        // Select strategy and compute similarities
        RaterSimilarityStrategy strategy = strategyFactory.getStrategy(
                similarityProperties.getStrategy());

        List<SimilarItem> similarities = strategy.compute(
                currentRaterId,
                normalizedRatings,
                ratingRepository);

        log.info("Computed {} similar raters for rater {}", similarities.size(), currentRaterId);
        return similarities;
    }

    /**
     * Get top-N similar raters for a given rater.
     * Alias for getSimilarRaters() - actual top-N filtering is done by the
     * strategy.
     *
     * @param currentRaterId the rater to find similarities for
     * @return list of similar items (rater IDs with similarity scores)
     */
    public List<SimilarItem> getTopNSimilarRaters(Integer currentRaterId) {
        return this.getSimilarRaters(currentRaterId);
    }
}
