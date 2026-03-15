package com.example.recommendation.service.similarity;

import java.util.Map;

/**
 * Utility class for similarity calculations.
 * Encapsulates the dot product calculation logic for ratings.
 */
public class SimilarityCalculator {

    private SimilarityCalculator() {
        // utility class
    }

    /**
     * Compute dot product between two normalized rating maps.
     * Only includes movies that both raters have rated.
     *
     * @param currentRatings normalized ratings of current rater (rating - 5)
     * @param otherRatings   normalized ratings of other rater (rating - 5)
     * @return dot product of the two rating vectors
     */
    public static double computeDotProduct(Map<Integer, Double> currentRatings,
            Map<Integer, Double> otherRatings) {
        double dotProduct = 0.0;

        for (Map.Entry<Integer, Double> entry : currentRatings.entrySet()) {
            Integer movieId = entry.getKey();
            Double currentRating = entry.getValue();

            Double otherRating = otherRatings.get(movieId);
            if (otherRating != null) {
                dotProduct += currentRating * otherRating;
            }
        }

        return dotProduct;
    }
}
