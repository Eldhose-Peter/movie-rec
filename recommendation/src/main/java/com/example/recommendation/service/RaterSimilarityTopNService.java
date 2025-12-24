package com.example.recommendation.service;

import com.example.recommendation.model.ImdbRatingEvent;
import com.example.recommendation.model.SimilarItem;
import com.example.recommendation.repository.RatingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/*
    Holds only the similarity of top 50 users

    Improvement
    - Sorting becomes less CPU instensive

    Issues
    - High memory usage
 */

@Slf4j
@Service
public class RaterSimilarityTopNService {

    private final RatingRepository ratingRepository;
    private static final int TOP_N = 50; // adjust as needed

    public RaterSimilarityTopNService(RatingRepository ratingRepository){
        this.ratingRepository = ratingRepository;
    }

    public List<SimilarItem> getTopNSimilarRaters(Integer currentRaterId){
        log.info("Fetching all ratings from DB");
        List<ImdbRatingEvent> allRatings = ratingRepository.findAll(); // single query

        // Group ratings by raterId
        Map<Integer, List<ImdbRatingEvent>> ratingsByRater = allRatings.stream()
                .collect(Collectors.groupingBy(ImdbRatingEvent::getRaterId));

        // Get current rater ratings
        List<ImdbRatingEvent> currentRatings = ratingsByRater.getOrDefault(currentRaterId, Collections.emptyList());
        Map<Integer, Double> currentRatingsMap = currentRatings.stream()
                .collect(Collectors.toMap(ImdbRatingEvent::getMovieId, r -> r.getRating() - 5));

        // Min-heap to store top-N similar raters efficiently
        PriorityQueue<SimilarItem> topSimilar = new PriorityQueue<>(Comparator.comparing(SimilarItem::getSimilarity));

        ratingsByRater.entrySet()
                .parallelStream()
                .filter(entry -> !entry.getKey().equals(currentRaterId))
                .forEach(entry -> {
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

                    if (dot > 0) { // optional: ignore zero similarities
                        synchronized (topSimilar) { // safely update min-heap across threads
                            if (topSimilar.size() < TOP_N) {
                                topSimilar.add(new SimilarItem(otherRaterId, dot));
                            } else if (dot > topSimilar.peek().getSimilarity()) {
                                topSimilar.poll();
                                topSimilar.add(new SimilarItem(otherRaterId, dot));
                            }
                        }
                    }
                });

        // Convert heap to descending sorted list
        List<SimilarItem> result = new ArrayList<>(topSimilar);
        result.sort(Comparator.comparing(SimilarItem::getSimilarity).reversed());

        log.info("Top {} similar raters computed", TOP_N);
        return result;
    }
}
