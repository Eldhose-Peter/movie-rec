package com.example.recommendation.service;

import com.example.recommendation.model.RatingEvent;
import com.example.recommendation.model.SimilarItem;
import com.example.recommendation.repository.RatingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*
     Stream and process as batch by rater
 */

@Slf4j
@Service
public class RaterSimilarityStreamService2 {

    private final RatingRepository ratingRepository;
    private static final int TOP_N = 50;

    public RaterSimilarityStreamService2(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @Transactional(readOnly = true)
    public List<SimilarItem> getTopNSimilarRaters(Integer currentRaterId) {
        log.info("Fetching ratings for current rater {}", currentRaterId);

        Map<Integer, Double> currentRatingsMap = ratingRepository.findById_RaterId(currentRaterId).stream()
                .collect(Collectors.toMap(RatingEvent::getMovieId, r -> r.getRating() - 5));

        if (currentRatingsMap.isEmpty()) {
            log.warn("No ratings found for rater {}", currentRaterId);
            return Collections.emptyList();
        }

        PriorityQueue<SimilarItem> topSimilar = new PriorityQueue<>(Comparator.comparing(SimilarItem::getSimilarity));

        try (Stream<RatingEvent> stream = ratingRepository.streamAllExcept(currentRaterId)) {
            List<RatingEvent> buffer = new ArrayList<>();
            Integer lastRater = null;

            for (Iterator<RatingEvent> it = stream.iterator(); it.hasNext();) {
                RatingEvent rating = it.next();
                if (lastRater == null) {
                    lastRater = rating.getRaterId();
                }

                if (!rating.getRaterId().equals(lastRater)) {
                    // flush the previous rater
                    processSingleRater(lastRater, buffer, currentRatingsMap, topSimilar);
                    buffer.clear();
                    lastRater = rating.getRaterId();
                }

                buffer.add(rating);
            }

            // flush the last rater
            if (!buffer.isEmpty()) {
                processSingleRater(lastRater, buffer, currentRatingsMap, topSimilar);
            }
        }

        List<SimilarItem> result = new ArrayList<>(topSimilar);
        result.sort(Comparator.comparing(SimilarItem::getSimilarity).reversed());
        return result;
    }

    private void processSingleRater(Integer raterId,
                                    List<RatingEvent> ratings,
                                    Map<Integer, Double> currentRatingsMap,
                                    PriorityQueue<SimilarItem> topSimilar) {
        double dot = 0.0;
        for (RatingEvent rating : ratings) {
            Double cur = currentRatingsMap.get(rating.getMovieId());
            if (cur != null) {
                dot += cur * (rating.getRating() - 5);
            }
        }
        if (dot > 0) {
            if (topSimilar.size() < TOP_N) {
                topSimilar.add(new SimilarItem(raterId, dot));
            } else if (dot > topSimilar.peek().getSimilarity()) {
                topSimilar.poll();
                topSimilar.add(new SimilarItem(raterId, dot));
            }
        }
    }

}
