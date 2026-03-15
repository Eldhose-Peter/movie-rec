package com.example.recommendation.service.strategy;

import com.example.recommendation.model.ImdbRatingEvent;
import com.example.recommendation.model.SimilarItem;
import com.example.recommendation.repository.RatingRepository;
import com.example.recommendation.service.RatingNormalizer;
import com.example.recommendation.service.SimilarityCalculator;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Strategy that uses OFFSET/LIMIT pagination to fetch ratings in batches.
 * Maintains a min-heap to keep only top-N similarities.
 *
 * Pros:
 * - Simple to understand and implement
 * - Easy to control batch size and retry logic
 * - Works in stateless environments (can stop/restart from offset)
 * - Bounded memory usage with top-N heap
 *
 * Cons:
 * - OFFSET cost grows linearly (DB must scan and discard skipped rows)
 * - Repetitive scans: Every batch re-scans the index/table
 * - Slow with very large tables (e.g., OFFSET 1M LIMIT 1K is slow)
 * - Inconsistent results if data changes during pagination
 */
@Slf4j
public class OffsetLimitStrategy implements RaterSimilarityStrategy {

    private static final int DEFAULT_BATCH_SIZE = 5000;
    private static final int DEFAULT_TOP_N = 50;
    private int batchSize;
    private int topN;

    public OffsetLimitStrategy() {
        this.batchSize = DEFAULT_BATCH_SIZE;
        this.topN = DEFAULT_TOP_N;
    }

    public OffsetLimitStrategy(int batchSize, int topN) {
        this.batchSize = batchSize;
        this.topN = topN;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public void setTopN(int topN) {
        this.topN = topN;
    }

    @Override
    public List<SimilarItem> compute(Integer currentRaterId,
            Map<Integer, Double> currentRatingsMap,
            RatingRepository repository) {
        log.info("Starting OffsetLimitStrategy for rater {} with batch size {} and top-N {}",
                currentRaterId, batchSize, topN);

        // Min-heap to keep only top-N similarities across all batches
        PriorityQueue<SimilarItem> topSimilar = new PriorityQueue<>(
                Comparator.comparing(SimilarItem::getSimilarity));

        int offset = 0;
        while (true) {
            // Fetch batch of ratings
            List<ImdbRatingEvent> batchRatings = repository.findBatch(offset, batchSize);
            if (batchRatings.isEmpty()) {
                break; // no more data
            }

            log.debug("Fetched batch at offset {} with {} ratings", offset, batchRatings.size());

            // Group ratings by rater in this batch
            Map<Integer, List<ImdbRatingEvent>> batchGrouped = batchRatings.stream()
                    .filter(r -> !r.getRaterId().equals(currentRaterId)) // skip current rater
                    .collect(Collectors.groupingBy(ImdbRatingEvent::getRaterId));

            // Compute similarity for each rater in batch
            for (Map.Entry<Integer, List<ImdbRatingEvent>> entry : batchGrouped.entrySet()) {
                Integer otherRaterId = entry.getKey();
                Map<Integer, Double> otherRatingsMap = RatingNormalizer.normalize(
                        entry.getValue());

                double similarity = SimilarityCalculator.computeDotProduct(
                        currentRatingsMap, otherRatingsMap);

                if (similarity > 0) {
                    if (topSimilar.size() < topN) {
                        topSimilar.add(new SimilarItem(otherRaterId, similarity));
                    } else if (similarity > topSimilar.peek().getSimilarity()) {
                        topSimilar.poll();
                        topSimilar.add(new SimilarItem(otherRaterId, similarity));
                    }
                }
            }

            offset += batchSize;
        }

        // Convert min-heap to descending sorted list
        List<SimilarItem> result = new ArrayList<>(topSimilar);
        result.sort(Comparator.comparing(SimilarItem::getSimilarity).reversed());

        log.info("OffsetLimitStrategy computed {} top similarities", result.size());
        return result;
    }
}
