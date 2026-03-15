package com.example.recommendation.service;

import com.example.recommendation.model.InternalRatingEvent;
import com.example.recommendation.service.recommendation.LSHBucketService;
import com.example.recommendation.service.recommendation.RatingPersistenceService;
import com.example.recommendation.service.recommendation.RecommendationGenerationService;
import com.example.recommendation.service.recommendation.SimilarityCandidateService;
import com.example.recommendation.service.recommendation.UserSignatureService;
import com.example.recommendation.service.recommendation.UserSimilarityComputationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Orchestrator service for incremental recommendation processing.
 * Coordinates the workflow of updating recommendations when new ratings are
 * added.
 *
 * Workflow:
 * 1. Persist new rating
 * 2. Compute/update user signature
 * 3. Update LSH buckets
 * 4. Generate candidate pairs
 * 5. Compute similarity scores
 * 6. Refresh recommendations
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class IncrementalRecommendationOrchestrator {

    private final RatingPersistenceService ratingPersistenceService;
    private final UserSignatureService userSignatureService;
    private final LSHBucketService lshBucketService;
    private final SimilarityCandidateService similarityCandidateService;
    private final UserSimilarityComputationService userSimilarityComputationService;
    private final RecommendationGenerationService recommendationGenerationService;

    /**
     * Process a new rating and update recommendations incrementally.
     * This is the main entry point for handling real-time/incremental ratings.
     *
     * @param rating the new rating event to process
     */
    @Transactional
    public void processNewRating(InternalRatingEvent rating) {
        Integer userId = rating.getRaterId();
        log.info("Processing new rating from user {}: movie {}, rating {}",
                userId, rating.getMovieId(), rating.getRating());

        try {
            // Step 1: Persist the new rating
            log.debug("Step 1/6: Persisting new rating for user {}", userId);
            ratingPersistenceService.saveInternalRating(rating);

            // Step 2: Update user signature (MinHash)
            log.debug("Step 2/6: Computing new signature for user {}", userId);
            int[] newSignature = userSignatureService.computeUserSignature(userId);

            // Step 3: Update LSH buckets
            log.debug("Step 3/6: Updating LSH buckets for user {}", userId);
            lshBucketService.updateUserBuckets(userId, newSignature);

            // Step 4: Generate candidate similarity pairs
            log.debug("Step 4/6: Generating similarity candidates for user {}", userId);
            similarityCandidateService.generateCandidatesForUser(userId);

            // Step 5: Compute similarity scores
            log.debug("Step 5/6: Computing similarities for user {}", userId);
            List<Integer> candidates = similarityCandidateService.getCandidatesForUser(userId);
            userSimilarityComputationService.computeAndSaveSimilarities(userId, candidates);

            // Step 6: Refresh recommendations
            log.debug("Step 6/6: Refreshing recommendations for user {}", userId);
            recommendationGenerationService.refreshUserRecommendations(userId);

            log.info("Successfully processed new rating for user {}", userId);

        } catch (Exception e) {
            log.error("Error processing new rating for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Refresh recommendations for a specific user without processing a new rating.
     * Useful for manual/scheduled recommendation updates.
     *
     * @param userId the user ID
     */
    @Transactional
    public void refreshRecommendations(int userId) {
        log.info("Refreshing recommendations for user {}", userId);
        try {
            recommendationGenerationService.refreshUserRecommendations(userId);
            log.info("Successfully refreshed recommendations for user {}", userId);
        } catch (Exception e) {
            log.error("Error refreshing recommendations for user {}: {}", userId, e.getMessage(), e);
            throw e;
        }
    }
}
