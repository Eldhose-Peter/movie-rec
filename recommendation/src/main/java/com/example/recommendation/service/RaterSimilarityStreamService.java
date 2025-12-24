package com.example.recommendation.service;

import com.example.recommendation.model.ImdbRatingEvent;
import com.example.recommendation.model.SimilarItem;
import com.example.recommendation.repository.RatingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/*

    Stream and process by batch of 1000 row
     -  Issue : Dot product of one rater can be in 2 separate batches.

    No OFFSET overhead
    Rows are consumed in order, no need to rescan previous rows.
    Much faster for large datasets.
    Constant memory usage in DB & app
    DB fetches rows in chunks (e.g., 2k–10k rows per network roundtrip).
    Your app can process one row at a time or in micro-batches.
    Single query, continuous stream
    Reduces repeated parsing, planning, and execution overhead.
    DB only sorts once at the start (because of ORDER BY).
    Stable performance even at row 10 million.

    ❌ Cons
    Long-lived transaction
    Streaming requires an open DB cursor inside a transaction.
    If the app crashes, you can’t easily restart in the middle (unlike offset).
    Resource locking
    A DB connection is held open until the stream closes.
    Fewer connections available in the pool.
    Error recovery is harder
    If something fails mid-stream, you must restart from the beginning (or checkpoint manually by remembering the last processed key).
 */

@Slf4j
@Service
public class RaterSimilarityStreamService {

    private final RatingRepository ratingRepository;
    private static final int TOP_N = 50;

    public RaterSimilarityStreamService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @Transactional(readOnly = true) // needed for streaming
    public List<SimilarItem> getTopNSimilarRaters(Integer currentRaterId) {
        log.info("Fetching ratings for current rater {}", currentRaterId);

        // Load current rater’s ratings into memory once
        Map<Integer, Double> currentRatingsMap = ratingRepository.findById_RaterId(currentRaterId).stream()
                .collect(Collectors.toMap(ImdbRatingEvent::getMovieId, r -> r.getRating() - 5));

        if (currentRatingsMap.isEmpty()) {
            log.warn("No ratings found for rater {}", currentRaterId);
            return Collections.emptyList();
        }

        log.info("Loaded {} ratings for current rater", currentRatingsMap.size());

        // PriorityQueue (min-heap) for top-N similarities
        PriorityQueue<SimilarItem> topSimilar = new PriorityQueue<>(Comparator.comparing(SimilarItem::getSimilarity));

        try (Stream<ImdbRatingEvent> stream = ratingRepository.streamAllExcept(currentRaterId)) {
            Map<Integer, List<ImdbRatingEvent>> grouped = new HashMap<>();

            stream.forEach(rating -> {
                grouped.computeIfAbsent(rating.getRaterId(), k -> new ArrayList<>()).add(rating);

                // When raterId group is "complete", process it
                if (grouped.size() > 1000) { // flush after ~1000 raters
                    processBatch(grouped, currentRatingsMap, topSimilar);
                    grouped.clear();
                }
            });

            // Final flush
            if (!grouped.isEmpty()) {
                processBatch(grouped, currentRatingsMap, topSimilar);
            }
        }

        // Convert heap to sorted list
        List<SimilarItem> result = new ArrayList<>(topSimilar);
        result.sort(Comparator.comparing(SimilarItem::getSimilarity).reversed());

        log.info("Top {} similar raters computed", TOP_N);
        return result;
    }

    private void processBatch(Map<Integer, List<ImdbRatingEvent>> batch,
                              Map<Integer, Double> currentRatingsMap,
                              PriorityQueue<SimilarItem> topSimilar) {
        for (Map.Entry<Integer, List<ImdbRatingEvent>> entry : batch.entrySet()) {
            Integer otherRaterId = entry.getKey();
            List<ImdbRatingEvent> otherRatings = entry.getValue();

            double dot = 0.0;
            for (ImdbRatingEvent rating : otherRatings) {
                Double cur = currentRatingsMap.get(rating.getMovieId());
                if (cur != null) {
                    dot += cur * (rating.getRating() - 5);
                }
            }

            if (dot > 0) {
                if (topSimilar.size() < TOP_N) {
                    topSimilar.add(new SimilarItem(otherRaterId, dot));
                } else if (dot > topSimilar.peek().getSimilarity()) {
                    topSimilar.poll();
                    topSimilar.add(new SimilarItem(otherRaterId, dot));
                }
            }
        }
        log.info("Processed {} raters in batch", batch.size());
    }
}
