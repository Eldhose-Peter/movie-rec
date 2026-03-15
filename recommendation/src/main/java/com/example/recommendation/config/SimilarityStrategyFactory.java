package com.example.recommendation.config;

import com.example.recommendation.service.strategy.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Factory for creating similarity calculation strategies.
 * Manages the creation and selection of different strategy implementations.
 */
@Slf4j
@Component
public class SimilarityStrategyFactory {

    private final MultiQueryStrategy multiQueryStrategy;
    private final LoadAllStrategy loadAllStrategy;
    private final StreamBatchCountStrategy streamBatchCountStrategy;
    private final StreamBatchRaterStrategy streamBatchRaterStrategy;
    private final OffsetLimitStrategy offsetLimitStrategy;
    private final LSHApproximateStrategy lshApproximateStrategy;

    public SimilarityStrategyFactory(SimilarityProperties properties) {
        this.multiQueryStrategy = new MultiQueryStrategy();

        this.loadAllStrategy = new LoadAllStrategy();

        this.streamBatchCountStrategy = new StreamBatchCountStrategy(
                properties.getStreamBatchCount());

        StreamBatchRaterStrategy streamRaterStrat = new StreamBatchRaterStrategy(
                properties.getTopN());
        this.streamBatchRaterStrategy = streamRaterStrat;

        this.offsetLimitStrategy = new OffsetLimitStrategy(
                properties.getBatchSize(),
                properties.getTopN());

        this.lshApproximateStrategy = new LSHApproximateStrategy(
                128,
                properties.getLshBands(),
                properties.getLshRowsPerBand());

        log.info("SimilarityStrategyFactory initialized with strategies: " +
                "multi_query, load_all, stream_batch_count, stream_batch_rater, offset_limit, lsh_approximate");
    }

    /**
     * Get a strategy by name.
     *
     * @param strategyName the strategy name (case-insensitive)
     * @return the strategy implementation
     * @throws IllegalArgumentException if strategy name is not found
     */
    public RaterSimilarityStrategy getStrategy(String strategyName) {
        if (strategyName == null || strategyName.isBlank()) {
            log.warn("Strategy name is null or blank, using default: stream_batch_rater");
            return streamBatchRaterStrategy;
        }

        return switch (strategyName.toLowerCase().trim()) {
            case "multi_query" -> {
                log.debug("Selected strategy: MultiQueryStrategy");
                yield multiQueryStrategy;
            }
            case "load_all" -> {
                log.debug("Selected strategy: LoadAllStrategy");
                yield loadAllStrategy;
            }
            case "stream_batch_count" -> {
                log.debug("Selected strategy: StreamBatchCountStrategy");
                yield streamBatchCountStrategy;
            }
            case "stream_batch_rater" -> {
                log.debug("Selected strategy: StreamBatchRaterStrategy");
                yield streamBatchRaterStrategy;
            }
            case "offset_limit" -> {
                log.debug("Selected strategy: OffsetLimitStrategy");
                yield offsetLimitStrategy;
            }
            case "lsh_approximate" -> {
                log.debug("Selected strategy: LSHApproximateStrategy");
                yield lshApproximateStrategy;
            }
            default -> {
                log.warn("Unknown strategy name: {}. Using default: stream_batch_rater", strategyName);
                yield streamBatchRaterStrategy;
            }
        };
    }
}
