package com.example.recommendation.service.strategy;

import com.example.recommendation.model.ImdbRatingEvent;
import com.example.recommendation.model.SimilarItem;
import com.example.recommendation.repository.RatingRepository;
import com.example.recommendation.service.similarity.RatingNormalizer;
import com.example.recommendation.service.similarity.SimilarityCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Stream;

/**
 * Strategy that streams ratings from database and processes in batches by
 * count.
 * Maintains a min-heap to keep only top-N similarities.
 *
 * Pros:
 * - Constant memory usage (heap fixed at top-N size)
 * - Single query with streaming
 * - No repeated DB scans
 * - Stable performance even with huge datasets
 *
 * Cons:
 * - Issue: Dot product of a single rater can be split across multiple batches
 * - Long-lived transaction holds DB connection
 * - Error recovery is harder
 */
@Slf4j
public class StreamBatchCountStrategy implements RaterSimilarityStrategy {

    private static final int DEFAULT_BATCH_COUNT = 1000;
    private int batchCount;

    public StreamBatchCountStrategy() {
        this.batchCount = DEFAULT_BATCH_COUNT;
    }

    public StreamBatchCountStrategy(int batchCount) {
        this.batchCount = batchCount;
    }

    public void setBatchCount(int batchCount) {
        this.batchCount = batchCount;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimilarItem> compute(Integer currentRaterId,
            Map<Integer, Double> currentRatingsMap,
            RatingRepository repository) {
        log.info("Starting StreamBatchCountStrategy for rater {} with batch count {}",
                currentRaterId, batchCount);

        // Min-heap to keep only top-N similarities
        PriorityQueue<SimilarItem> topSimilar = new PriorityQueue<>(
                Comparator.comparing(SimilarItem::getSimilarity));

        try (Stream<ImdbRatingEvent> stream = repository.streamAllExcept(currentRaterId)) {
            Map<Integer, List<ImdbRatingEvent>> grouped = new HashMap<>();

            stream.forEach(rating -> {
                grouped.computeIfAbsent(rating.getRaterId(), k -> new ArrayList<>())
                        .add(rating);

                // Process batch when we have accumulated enough raters
                if (grouped.size() > batchCount) {
                    processBatch(grouped, currentRatingsMap, topSimilar);
                    grouped.clear();
                }
            });

            // Process remaining ratings in final batch
            if (!grouped.isEmpty()) {
                processBatch(grouped, currentRatingsMap, topSimilar);
            }
        }

        // Convert min-heap to descending sorted list
        List<SimilarItem> result = new ArrayList<>(topSimilar);
        result.sort(Comparator.comparing(SimilarItem::getSimilarity).reversed());

        log.info("StreamBatchCountStrategy computed {} top similarities", result.size());
        return result;
    }

    private void processBatch(Map<Integer, List<ImdbRatingEvent>> batch,
            Map<Integer, Double> currentRatingsMap,
            PriorityQueue<SimilarItem> topSimilar) {
        for (Map.Entry<Integer, List<ImdbRatingEvent>> entry : batch.entrySet()) {
            Integer otherRaterId = entry.getKey();
            Map<Integer, Double> otherRatingsMap = RatingNormalizer.normalize(
                    entry.getValue());

            double similarity = SimilarityCalculator.computeDotProduct(
                    currentRatingsMap, otherRatingsMap);

            if (similarity > 0) {
                if (topSimilar.size() < batchCount) {
                    topSimilar.add(new SimilarItem(otherRaterId, similarity));
                } else if (similarity > topSimilar.peek().getSimilarity()) {
                    topSimilar.poll();
                    topSimilar.add(new SimilarItem(otherRaterId, similarity));
                }
            }
        }
        log.debug("Processed {} raters in batch", batch.size());
    }
}
