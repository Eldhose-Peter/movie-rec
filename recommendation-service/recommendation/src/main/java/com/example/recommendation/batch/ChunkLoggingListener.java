package com.example.recommendation.batch;

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class ChunkLoggingListener implements ChunkListener {

    private static final Logger log = LoggerFactory.getLogger(ChunkLoggingListener.class);

    @Override
    public void beforeChunk(ChunkContext context) {
        // Optional: called before processing a chunk
        log.info("Processing next set of chunks");
    }

    @Override
    public void afterChunk(ChunkContext context) {
        long readCount = context.getStepContext().getStepExecution().getReadCount();
        long writeCount = context.getStepContext().getStepExecution().getWriteCount();
        long commitCount = context.getStepContext().getStepExecution().getCommitCount();

        log.info("Chunk processed: commits={}, itemsRead={}, itemsWritten={}",
                commitCount, readCount, writeCount);
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        log.warn("Chunk processing failed");
    }
}

