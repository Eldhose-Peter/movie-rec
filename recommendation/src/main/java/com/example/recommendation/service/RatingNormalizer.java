package com.example.recommendation.service;

import com.example.recommendation.model.ImdbRatingEvent;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility class for normalizing ratings.
 * Converts ratings to a normalized scale (rating - 5) for similarity
 * calculations.
 */
public class RatingNormalizer {

    private RatingNormalizer() {
        // utility class
    }

    /**
     * Normalize a list of rating events to a map of movieId -> normalized rating.
     * Normalization: rating - 5 (converts from 0-10 scale to -5 to 5 scale)
     *
     * @param ratings list of rating events
     * @return map of movieId to normalized rating
     */
    public static Map<Integer, Double> normalize(List<ImdbRatingEvent> ratings) {
        return ratings.stream()
                .collect(Collectors.toMap(
                        ImdbRatingEvent::getMovieId,
                        rating -> rating.getRating() - 5));
    }
}
