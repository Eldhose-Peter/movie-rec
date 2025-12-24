package com.example.recommendation.service;

import com.example.recommendation.model.ImdbRatingEvent;
import com.example.recommendation.model.SimilarItem;
import com.example.recommendation.repository.RatingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*
    Improvement:
    - Reduces DB transaction to a single query

    Issues:
    - High memory usage
    - Memory scales with total ratings:
    - 13M rows × ~50 bytes/row = ~650 MB (rough estimate, just for objects).
    - parallelStream spawns multiple threads:
      Each thread creates its own otherRatingsMap, increasing peak memory usage.
       sorted() creates an additional structure for sorting 138k SimilarItems.
    - this single query could return millions of rows (e.g., 138k raters × ~100–200 ratings each = ~13–27M rows).
    - Large result sets can overwhelm network bandwidth or the DB driver.

 */

@Slf4j
@Service
public class RaterSimilarityService2 {
    private final RatingRepository ratingRepository;

    public RaterSimilarityService2(RatingRepository ratingRepository){
        this.ratingRepository = ratingRepository;
    }

    /**
     * Compute all similarities of current rater with others.
     */
    public List<SimilarItem> getSimilarRaters(Integer currentRaterId){
        log.info("Fetching all ratings from DB");
        List<ImdbRatingEvent> allRatings = ratingRepository.findAll(); // single query
        log.info("Fetched all ratings from DB");

        // Group ratings by raterId
        Map<Integer, List<ImdbRatingEvent>> ratingsByRater = allRatings.stream()
                .collect(Collectors.groupingBy(ImdbRatingEvent::getRaterId));

        // Get current rater ratings
        List<ImdbRatingEvent> currentRatings = ratingsByRater.getOrDefault(currentRaterId, Collections.emptyList());
        Map<Integer, Double> currentRatingsMap = currentRatings.stream()
                .collect(Collectors.toMap(ImdbRatingEvent::getMovieId, r -> r.getRating() - 5)); // scale -5 to 5

        log.info("Calculating dot product");
        // Compute dot products in parallel
        List<SimilarItem> similarRaters = ratingsByRater.entrySet()
                .parallelStream()
                .filter(entry -> !entry.getKey().equals(currentRaterId))
                .map(entry -> {
                    Integer otherRaterId = entry.getKey();
                    List<ImdbRatingEvent> otherRatings = entry.getValue();

                    Map<Integer, Double> otherRatingsMap = otherRatings.stream()
                            .collect(Collectors.toMap(ImdbRatingEvent::getMovieId, r -> r.getRating() - 5));

                    double dot = 0.0;
                    for (Map.Entry<Integer, Double> e : currentRatingsMap.entrySet()) {
                        Double otherRating = otherRatingsMap.get(e.getKey());
                        if (otherRating != null) {
                            dot += e.getValue() * otherRating;
                        }
                    }
                    return new SimilarItem(otherRaterId, dot);
                })
                .sorted(Comparator.comparing(SimilarItem::getSimilarity).reversed()) // descending
                .toList();

        log.info("Similarities computed: {}", similarRaters.size());
        return similarRaters;
    }
}
