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
 * Strategy that streams ratings from database and processes in batches by rater
 * ID.
 * Assumes data is ordered by rater_id, processes complete rater at a time.
 * Maintains a min-heap to keep only top-N similarities.
 *
 * Pros:
 * - Constant memory usage (heap fixed at top-N size)
 * - Single query with streaming
 * - Processes each rater atomically (not split across batches)
 * - No repeated DB scans
 * - Stable performance even with huge datasets
 *
 * Cons:
 * - Long-lived transaction holds DB connection
 * - Error recovery is harder
 * - Depends on data being ordered by rater_id
 */
@Slf4j
public class StreamBatchRaterStrategy implements RaterSimilarityStrategy {

    private static final int DEFAULT_TOP_N = 50;
    private int topN;

    public StreamBatchRaterStrategy() {
        this.topN = DEFAULT_TOP_N;
    }

    public StreamBatchRaterStrategy(int topN) {
        this.topN = topN;
    }

    public void setTopN(int topN) {
        this.topN = topN;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimilarItem> compute(Integer currentRaterId,
            Map<Integer, Double> currentRatingsMap,
            RatingRepository repository) {
        log.info("Starting StreamBatchRaterStrategy for rater {} with top-N = {}",
                currentRaterId, topN);

        // Min-heap to keep only top-N similarities
        PriorityQueue<SimilarItem> topSimilar = new PriorityQueue<>(
                Comparator.comparing(SimilarItem::getSimilarity));

        try (Stream<ImdbRatingEvent> stream = repository.streamAllExcept(currentRaterId)) {
            List<ImdbRatingEvent> buffer = new ArrayList<>();
            Integer lastRaterId = null;

            for (Iterator<ImdbRatingEvent> it = stream.iterator(); it.hasNext();) {
                ImdbRatingEvent rating = it.next();
                if (lastRaterId == null) {
                    lastRaterId = rating.getRaterId();
                }

                // When rater changes, process the previous rater's data
                if (!rating.getRaterId().equals(lastRaterId)) {
                    processSingleRater(lastRaterId, buffer, currentRatingsMap, topSimilar);
                    buffer.clear();
                    lastRaterId = rating.getRaterId();
                }

                buffer.add(rating);
            }

            // Process the last rater
            if (!buffer.isEmpty()) {
                processSingleRater(lastRaterId, buffer, currentRatingsMap, topSimilar);
            }
        }

        // Convert min-heap to descending sorted list
        List<SimilarItem> result = new ArrayList<>(topSimilar);
        result.sort(Comparator.comparing(SimilarItem::getSimilarity).reversed());

        log.info("StreamBatchRaterStrategy computed {} top similarities", result.size());
        return result;
    }

    private void processSingleRater(Integer raterId,
            List<ImdbRatingEvent> ratings,
            Map<Integer, Double> currentRatingsMap,
            PriorityQueue<SimilarItem> topSimilar) {
        Map<Integer, Double> otherRatingsMap = RatingNormalizer.normalize(ratings);

        double similarity = SimilarityCalculator.computeDotProduct(
                currentRatingsMap, otherRatingsMap);

        if (similarity > 0) {
            if (topSimilar.size() < topN) {
                topSimilar.add(new SimilarItem(raterId, similarity));
            } else if (similarity > topSimilar.peek().getSimilarity()) {
                topSimilar.poll();
                topSimilar.add(new SimilarItem(raterId, similarity));
            }
        }
    }
}
