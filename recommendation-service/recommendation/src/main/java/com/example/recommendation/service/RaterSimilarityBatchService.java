package com.example.recommendation.service;

import com.example.recommendation.model.RatingEvent;
import com.example.recommendation.model.SimilarItem;
import com.example.recommendation.repository.RatingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/*
    Simple to understand and implement.
    Easy to control batch size (LIMIT) and retry if a batch fails.
    Works in stateless environments (you can stop/restart from a given offset).

    ❌ Cons
    OFFSET cost grows linearly
    OFFSET is not free: the database still scans and discards skipped rows.
    Example: OFFSET 1000000 LIMIT 1000 → DB scans 1,000,000 rows then returns 1000.
    This gets very slow with large tables.
    Repetitive scans
    Every batch re-scans the index/table to reach the offset.
    Inconsistent results if new rows are inserted/deleted while batching (unless snapshot isolation is used).
    Higher DB load → multiple queries, repeated sorting/ordering.

 */

@Slf4j
@Service
public class RaterSimilarityBatchService {

    private final RatingRepository ratingRepository;
    private static final int BATCH_SIZE = 5000; // tune this
    private static final int TOP_N = 50;        // keep only top 50 similar raters

    public RaterSimilarityBatchService(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    public List<SimilarItem> getTopNSimilarRaters(Integer currentRaterId) {
        log.info("Fetching ratings for current rater {}", currentRaterId);

        // Load only current rater ratings once
        List<RatingEvent> currentRatings = ratingRepository.findById_RaterId(currentRaterId);
        if (currentRatings.isEmpty()) {
            log.warn("No ratings found for rater {}", currentRaterId);
            return Collections.emptyList();
        }

        Map<Integer, Double> currentRatingsMap = currentRatings.stream()
                .collect(Collectors.toMap(RatingEvent::getMovieId, r -> r.getRating() - 5));

        log.info("Loaded {} ratings for current rater", currentRatingsMap.size());

        // PriorityQueue to keep top N results across all batches
        PriorityQueue<SimilarItem> topSimilar = new PriorityQueue<>(Comparator.comparing(SimilarItem::getSimilarity));

        int offset = 0;
        while (true) {
            // Fetch batch of ratings
            List<RatingEvent> batchRatings = ratingRepository.findBatch(offset, BATCH_SIZE);
            if (batchRatings.isEmpty()) {
                break; // no more data
            }

            // Group ratings by raterId inside batch
            Map<Integer, List<RatingEvent>> batchGrouped = batchRatings.stream()
                    .filter(r -> !r.getRaterId().equals(currentRaterId)) // skip current rater
                    .collect(Collectors.groupingBy(RatingEvent::getRaterId));

            // Compute similarity for each rater in the batch
            for (Map.Entry<Integer, List<RatingEvent>> entry : batchGrouped.entrySet()) {
                Integer otherRaterId = entry.getKey();
                List<RatingEvent> otherRatings = entry.getValue();

                double dot = 0.0;
                for (RatingEvent rating : otherRatings) {
                    Double curRating = currentRatingsMap.get(rating.getMovieId());
                    if (curRating != null) {
                        dot += curRating * (rating.getRating() - 5);
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

            offset += BATCH_SIZE;
            log.info("Processed {} ratings so far", offset);
        }

        // Convert heap to sorted list
        List<SimilarItem> result = new ArrayList<>(topSimilar);
        result.sort(Comparator.comparing(SimilarItem::getSimilarity).reversed());

        log.info("Top {} similar raters computed", TOP_N);
        return result;
    }
}
