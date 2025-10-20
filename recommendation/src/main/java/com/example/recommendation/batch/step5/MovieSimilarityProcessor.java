package com.example.recommendation.batch.step5;

import com.example.recommendation.batch.step4.RatingsPrefetchListener;
import com.example.recommendation.model.MovieWeightContribution;
import com.example.recommendation.model.RatingEvent;
import com.example.recommendation.model.UserSimilarity;
import com.example.recommendation.repository.RatingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MovieSimilarityProcessor {

    @Bean
    @StepScope
    public ItemProcessor<UserSimilarity, List<MovieWeightContribution>> movieSimilarityPrefetchProcessor(RatingRepository ratingRepository, SimilaritiesPrefetchListener prefetchListener){
        return candidate -> {
            int r1Id = candidate.getRaterId();
            int r2Id = candidate.getOtherRaterId();
            double similarity = candidate.getSimilarityScore();

            // cached or loaded on demand
            Map<Integer, List<RatingEvent>> ratingsCache = prefetchListener.getRatingsCache();
            List<RatingEvent> ratings1 = ratingsCache.getOrDefault(r1Id, List.of());
            List<RatingEvent> ratings2 = ratingsCache.getOrDefault(r2Id, List.of());

            log.info("Ratings1 size {}, Ratings2 size {}", ratings1.size(), ratings2.size());

            // for each movie not watched by rater1,
            // calculate the weighted rating by multiplying with rater closeness
            Set<Integer> ratedByRater1 = ratings1.stream()
                    .map(RatingEvent::getMovieId)
                    .collect(Collectors.toSet());

            // Only movies rated by other user but not current rater
            List<MovieWeightContribution> contributions = ratings2.stream()
                    .filter(r -> !ratedByRater1.contains(r.getMovieId()))
                    .map(r -> new MovieWeightContribution(
                            r1Id,
                            r.getMovieId(),
                            r.getRating() * similarity, // weighted part
                            similarity                   // weight part
                    ))
                    .toList();

            log.debug("User {} vs {} produced {} contributions", r1Id, r2Id, contributions.size());

            return contributions;

        };
    }

}
