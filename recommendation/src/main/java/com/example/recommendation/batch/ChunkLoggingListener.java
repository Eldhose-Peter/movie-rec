package com.example.recommendation.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChunkLoggingListener implements ChunkListener {

    private long startTime;

    @Override
    public void beforeChunk(ChunkContext context) {
        startTime = System.currentTimeMillis();
        log.info("Starting chunk for step: {}", context.getStepContext().getStepName());
    }

    @Override
    public void afterChunk(ChunkContext context) {
        long duration = System.currentTimeMillis() - startTime;
        long readCount = context.getStepContext().getStepExecution().getReadCount();
        long writeCount = context.getStepContext().getStepExecution().getWriteCount();
        long commitCount = context.getStepContext().getStepExecution().getCommitCount();

        log.info("Completed chunk for step: {}", context.getStepContext().getStepName());
        log.info("Chunk processing duration: {}",duration);
        log.info("Chunk processed: commits={}, itemsRead={}, itemsWritten={}",
                commitCount, readCount, writeCount);
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        log.error("Error occurred in chunk for step: {}", context.getStepContext().getStepName());
    }
}

