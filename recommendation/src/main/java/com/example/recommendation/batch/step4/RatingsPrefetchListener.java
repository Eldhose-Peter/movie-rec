package com.example.recommendation.batch.step4;

import com.example.recommendation.model.RatingEvent;
import com.example.recommendation.model.UserSimilarityKey;
import com.example.recommendation.repository.RatingRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.ItemReadListener;
import org.springframework.batch.core.annotation.AfterChunk;
import org.springframework.batch.core.annotation.AfterChunkError;
import org.springframework.batch.core.annotation.BeforeChunk;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RatingsPrefetchListener implements ItemReadListener<UserSimilarityKey>, ChunkListener {

    private final Set<Integer> currentRaters = new HashSet<>();
    private final RatingRepository ratingRepository;

    @Getter
    private final Map<Integer, List<RatingEvent>> ratingsCache = new ConcurrentHashMap<>();

    public RatingsPrefetchListener(RatingRepository ratingRepository) {
        this.ratingRepository = ratingRepository;
    }

    @Override
    public void beforeRead() {
        //no-op
    }

    @Override
    public void afterRead(UserSimilarityKey item) {
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
        log.info("Pre-processing before chunk");
        currentRaters.clear();
        ratingsCache.clear();
    }

    @Override
    public void afterChunk(ChunkContext context) {
        log.info("Post-processing after chunk");
        // Fetch all ratings for users seen in this chunk
        long start =  System.currentTimeMillis();
        if (!currentRaters.isEmpty()) {
            Map<Integer, List<RatingEvent>> fetched = ratingRepository.findByRaterIds(currentRaters)
                    .stream()
                    .collect(Collectors.groupingBy(r -> r.getId().getRaterId()));

            ratingsCache.putAll(fetched);
        }
        log.info("Database fetch | delay {} | size {}", System.currentTimeMillis()-start, ratingsCache.size() );
        currentRaters.clear();
    }

    @Override
    public void afterChunkError(ChunkContext context) {
        currentRaters.clear();
    }
}
