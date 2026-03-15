package com.example.recommendation.service.strategy;

import com.example.recommendation.model.SimilarItem;
import com.example.recommendation.repository.RatingRepository;

import java.util.List;
import java.util.Map;

/**
 * Strategy interface for computing rater-to-rater similarity.
 * Different implementations use different data fetching approaches.
 */
public interface RaterSimilarityStrategy {

    /**
     * Compute similar raters for a given current rater.
     *
     * @param currentRaterId    the rater to find similarities for
     * @param currentRatingsMap normalized ratings of the current rater (rating - 5)
     * @param repository        rating repository for data access
     * @return list of similar items (rater IDs with similarity scores)
     */
    List<SimilarItem> compute(Integer currentRaterId,
            Map<Integer, Double> currentRatingsMap,
            RatingRepository repository);
}
