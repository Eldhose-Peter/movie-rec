package com.example.recommendation.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Component
@Data
public class SimilarityProperties {

    private String strategy = "stream_batch_rater";
    private int topN = 50;
    private int batchSize = 5000;
    private int streamBatchCount = 1000;
    private boolean parallelize = true;

    // LSH specific parameters
    private int lshBands = 32;
    private int lshRowsPerBand = 4;
}
