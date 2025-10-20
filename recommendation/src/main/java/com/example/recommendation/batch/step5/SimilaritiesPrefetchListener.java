package com.example.recommendation.batch.step5;

import com.example.recommendation.model.RatingEvent;
import com.example.recommendation.model.UserSimilarity;
import com.example.recommendation.repository.RatingJdbcRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class SimilaritiesPrefetchListener implements ItemReadListener<UserSimilarity>, ChunkListener, StepExecutionListener {

    private final Set<Integer> currentRaters = new HashSet<>();
    private final RatingJdbcRepository ratingRepository;

    @Getter
    private final Map<Integer, List<RatingEvent>> ratingsCache = new ConcurrentHashMap<>();

    public SimilaritiesPrefetchListener(RatingJdbcRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        ratingsCache.clear();
        currentRaters.clear();
    }

    @Override
    public void beforeRead() {
        //no-op
    }

    @Override
    public void afterRead(UserSimilarity item) {
        currentRaters.add(item.getRaterId());
        currentRaters.add(item.getOtherRaterId());
    }

    @Override
    public void onReadError(Exception ex) {
        currentRaters.clear();
    }

    // Called at chunk boundaries
    @Override
    public void beforeChunk(ChunkContext context) {

        ratingsCache.clear();
        long start =  System.currentTimeMillis();
        if (!currentRaters.isEmpty()) {
            Map<Integer, List<RatingEvent>> fetched = ratingRepository.getRatingsForRaters(currentRaters);

            ratingsCache.putAll(fetched);
        }
        log.info("Similarities prefetch - Database fetch | delay {} | size {}", System.currentTimeMillis()-start, ratingsCache.size() );
        currentRaters.clear();
    }

    @Override
    public void afterChunk(ChunkContext context) {
      // no-op
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        currentRaters.clear();
    }
}
