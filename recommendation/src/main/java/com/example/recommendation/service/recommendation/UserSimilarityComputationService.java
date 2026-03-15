package com.example.recommendation.service.recommendation;

import com.example.recommendation.model.ImdbRatingEvent;
import com.example.recommendation.model.InternalRatingEvent;
import com.example.recommendation.model.UserSimilarity;
import com.example.recommendation.repository.InternalRatingRepository;
import com.example.recommendation.repository.RatingRepository;
import com.example.recommendation.repository.UserSimilarityRepository;
import com.example.recommendation.service.lsh.Similarity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for computing and persisting user-to-user similarity scores.
 * Uses cosine similarity between rating vectors.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSimilarityComputationService {

    private final UserSimilarityRepository userSimilarityRepository;
    private final RatingRepository ratingRepository;
    private final InternalRatingRepository internalRatingRepository;

    /**
     * Compute similarities between a user and candidate users, then persist
     * results.
     * Deletes old similarities before computing new ones.
     *
     * @param userId     the primary user ID
     * @param candidates list of candidate user IDs to compare against
     */
    public void computeAndSaveSimilarities(int userId, List<Integer> candidates) {
        log.info("Computing similarities for user {} against {} candidates",
                userId, candidates.size());

        // Delete old similarities for this user
        log.debug("Deleting old similarities for user {}", userId);
        userSimilarityRepository.deleteByRaterId(userId);

        // Load primary user's ratings (from internal repository)
        List<InternalRatingEvent> userRatings = internalRatingRepository.findByRaterId(userId);
        if (userRatings.isEmpty()) {
            log.warn("No ratings found for user {}, skipping similarity computation", userId);
            return;
        }

        Map<Integer, Double> userRatingMap = userRatings.stream()
                .collect(Collectors.toMap(InternalRatingEvent::getMovieId, InternalRatingEvent::getRating));

        log.debug("User {} has {} ratings", userId, userRatingMap.size());

        // Compute similarity with each candidate
        for (Integer candidateId : candidates) {
            // Fetch candidate's ratings (from IMDB repository)
            List<ImdbRatingEvent> candidateRatings = ratingRepository.findById_RaterId(candidateId);

            if (candidateRatings.isEmpty()) {
                log.debug("No ratings found for candidate user {}, skipping", candidateId);
                continue;
            }

            Map<Integer, Double> candidateRatingMap = candidateRatings.stream()
                    .collect(Collectors.toMap(ImdbRatingEvent::getMovieId, ImdbRatingEvent::getRating));

            // Compute cosine similarity
            double similarity = Similarity.cosine(userRatingMap, candidateRatingMap);

            log.debug("Computed similarity between user {} and {}: {} (from {} and {} ratings)",
                    userId, candidateId, similarity, userRatingMap.size(), candidateRatingMap.size());

            // Persist only positive similarities
            if (similarity > 0) {
                UserSimilarity userSimilarity = new UserSimilarity(userId, candidateId, similarity);
                userSimilarityRepository.save(userSimilarity);
                log.debug("Saved similarity score for user pair ({}, {})", userId, candidateId);
            }
        }

        log.info("Completed similarity computation for user {} with {} candidates",
                userId, candidates.size());
    }
}
