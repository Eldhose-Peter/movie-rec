package com.example.recommendation.service.recommendation;

import com.example.recommendation.model.ImdbRatingEvent;
import com.example.recommendation.model.UserRecommendation;
import com.example.recommendation.model.UserSimilarity;
import com.example.recommendation.repository.InternalRatingRepository;
import com.example.recommendation.repository.RatingRepository;
import com.example.recommendation.repository.UserRecommendationRepository;
import com.example.recommendation.repository.UserSimilarityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating movie recommendations based on similar users' ratings.
 * Uses weighted aggregation from similar users to rank unseen movies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationGenerationService {

        private static final int DEFAULT_TOP_RECOMMENDATIONS = 20;

        private final UserSimilarityRepository userSimilarityRepository;
        private final RatingRepository ratingRepository;
        private final InternalRatingRepository internalRatingRepository;
        private final UserRecommendationRepository userRecommendationRepository;

        /**
         * Generate and refresh recommendations for a user.
         * Aggregates ratings from similar users, excluding already-rated movies.
         *
         * @param userId the user ID to generate recommendations for
         */
        @Transactional
        public void refreshUserRecommendations(int userId) {
                log.info("Refreshing recommendations for user {}", userId);

                // 1. Fetch similar users sorted by similarity score
                List<UserSimilarity> similarities = userSimilarityRepository
                                .findByIdRaterIdOrderBySimilarityScoreDesc(userId);
                log.info("Found {} similar users for user {}", similarities.size(), userId);

                if (similarities.isEmpty()) {
                        log.warn("No similar users found for user {}, clearing recommendations", userId);
                        userRecommendationRepository.deleteByIdUserId(userId);
                        return;
                }

                // Build similarity map: userId -> similarity score
                Map<Integer, Double> similarityMap = similarities.stream()
                                .collect(Collectors.toMap(
                                                s -> s.getId().getOtherRaterId(),
                                                UserSimilarity::getSimilarityScore));

                // 2. Get movies already rated by this user (to exclude from recommendations)
                Set<Integer> seenMovieIds = internalRatingRepository.findByRaterId(userId).stream()
                                .map(event -> Math.toIntExact(event.getMovieId()))
                                .collect(Collectors.toSet());
                log.debug("User {} has {} rated movies", userId, seenMovieIds.size());

                // 3. Fetch all ratings from similar users in a single batch
                List<ImdbRatingEvent> allSimilarUserRatings = ratingRepository
                                .findByRaterIds(similarityMap.keySet());
                log.debug("Fetched {} ratings from {} similar users",
                                allSimilarUserRatings.size(), similarityMap.size());

                // 4. Aggregate ratings in-memory using weighted sum
                Map<Integer, AggregatedRating> aggregationMap = new HashMap<>();

                for (ImdbRatingEvent rating : allSimilarUserRatings) {
                        // Skip movies already rated by the user
                        if (seenMovieIds.contains(rating.getMovieId())) {
                                continue;
                        }

                        double similarityScore = similarityMap.get(rating.getRaterId());
                        double weightedRating = rating.getRating() * similarityScore;

                        aggregationMap.computeIfAbsent(rating.getMovieId(), k -> new AggregatedRating())
                                        .addRating(weightedRating, similarityScore);

                        log.debug("Aggregated movie {}: weighted_sum={}, weight_sum={}",
                                        rating.getMovieId(),
                                        aggregationMap.get(rating.getMovieId()).weightedSum,
                                        aggregationMap.get(rating.getMovieId()).weightSum);
                }

                log.info("Aggregated {} candidate movies for user {}", aggregationMap.size(), userId);

                // 5. Transform aggregations to recommendations and sort by score
                List<UserRecommendation> recommendations = aggregationMap.entrySet().stream()
                                .map(entry -> {
                                        int movieId = entry.getKey();
                                        double weightedScore = entry.getValue().weightedSum;
                                        double normalizedScore = entry.getValue().weightSum;
                                        return new UserRecommendation((long) userId, (long) movieId, weightedScore,
                                                        normalizedScore);
                                })
                                .sorted((r1, r2) -> Double.compare(r2.getWeightedSumTotal(), r1.getWeightedSumTotal()))
                                .limit(DEFAULT_TOP_RECOMMENDATIONS)
                                .collect(Collectors.toList());

                log.info("Generated {} top recommendations for user {}", recommendations.size(), userId);

                // 6. Atomically replace old recommendations with new ones
                userRecommendationRepository.deleteByIdUserId(userId);
                userRecommendationRepository.saveAll(recommendations);

                log.info("Saved {} recommendations for user {}", recommendations.size(), userId);
        }

        /**
         * Internal value object for aggregating weighted ratings.
         */
        private static class AggregatedRating {
                double weightedSum = 0.0;
                double weightSum = 0.0;

                void addRating(double weighted, double weight) {
                        this.weightedSum += weighted;
                        this.weightSum += weight;
                }
        }
}
