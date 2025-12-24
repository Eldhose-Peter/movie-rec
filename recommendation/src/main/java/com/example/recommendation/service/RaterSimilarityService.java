package com.example.recommendation.service;

import com.example.recommendation.model.ImdbRatingEvent;
import com.example.recommendation.model.SimilarItem;
import com.example.recommendation.repository.RatingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/*
    Scalability issues with this approach

    Assume :
        - 138k unique raters
        - average ratings per user is 200

    1. DB overlaod
        - 138k requests
        - possible timeouts
        - parallel processing of DB requests leds to - thread starvation & connection pool starvation

    2. Memory consumtion
        - Memory per rating ~ 50 bytes
        - Memory per otherRatingsMap = 200 * 50 = 10 KB
        - If thread pool uses 100 threads, it could load 100 other raters’ ratings simultaneously → ~1 MB just for map

    3. Latency
        - 2 DB queries per rater pair (current rater + other rater)
        - Even if each query takes 10ms, for 138k raters:
        - 138k * 10ms ≈ 23 minutes!

    4. CPU Intensive
        - Sorting 138k elements in memory adds additional CPU overhead.

 */


@Slf4j
@Service
public class RaterSimilarityService {

    private final RatingRepository ratingRepository;

    RaterSimilarityService(RatingRepository ratingRepository){
        this.ratingRepository = ratingRepository;
    }

    public List<SimilarItem> getSimilarRaters(Integer currentRaterId){
        return this.calculateSimilarRaters(currentRaterId);
    }

    private List<SimilarItem> calculateSimilarRaters(Integer currentRaterId){
        log.info("Starting calculation for similarRaters");
        List<Integer> raterIds = this.ratingRepository.getUniqueRaterIds();

        log.info("Recieved" + raterIds.size() + "unique rater Ids from DB");

        List<SimilarItem> similarRaters = new ArrayList<>();

        similarRaters = raterIds
                .parallelStream()
                .filter(otherRaterId -> !Objects.equals(otherRaterId, currentRaterId))
                .map(otherRaterId -> {
                    Double value = this.dotProduct(currentRaterId, otherRaterId);
                    return new SimilarItem(otherRaterId, (double) value);
                })
                .sorted()
                .toList();

        return similarRaters;
    }

    private Double dotProduct(Integer curRater, Integer otherRater) {
        log.info("Calculating dotProduct for" + curRater + " x " + otherRater);
        // Fetch ratings from repository
        List<ImdbRatingEvent> curUserRatings = ratingRepository.findById_RaterId(curRater);
        List<ImdbRatingEvent> otherUserRatings = ratingRepository.findById_RaterId(otherRater);

        log.info("current user ratings : "+ curUserRatings.size());
        log.info("other user ratings : "+ otherUserRatings.size());

        // Map movieId -> rating for efficient lookup
        Map<Integer, Double> otherRatingsMap = otherUserRatings.stream()
                .collect(Collectors.toMap(ImdbRatingEvent::getMovieId, ImdbRatingEvent::getRating));

        Double dotProduct = 0.0;

        for (ImdbRatingEvent rating : curUserRatings) {
            Double curRating = rating.getRating();
            Double otherRating = otherRatingsMap.get(rating.getMovieId());

            if (otherRating != null) {
                // translate rating from 0 to 10 scale to -5 to 5 scale
                curRating -= 5;
                otherRating -= 5;

                dotProduct += curRating * otherRating;
            }
        }

        return dotProduct;
    }
}
