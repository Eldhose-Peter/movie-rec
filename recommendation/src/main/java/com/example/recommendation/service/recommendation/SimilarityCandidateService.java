package com.example.recommendation.service.recommendation;

import com.example.recommendation.repository.SimilarityCandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for managing similarity candidate pairs.
 * Candidates are potential similar users discovered through LSH bucketing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimilarityCandidateService {

    private final SimilarityCandidateRepository similarityCandidateRepository;

    /**
     * Generate and persist candidate similarity pairs for a user.
     * Deletes old candidates and regenerates from LSH buckets.
     *
     * @param userId the user ID for which to generate candidates
     */
    public void generateCandidatesForUser(int userId) {
        log.debug("Generating similarity candidates for user {}", userId);

        // Delete old candidates
        log.debug("Deleting old similarity candidates for user {}", userId);
        similarityCandidateRepository.deleteSimilarityCandidatesByUserId(userId);

        // Regenerate from LSH buckets and save to DB
        log.debug("Regenerating similarity candidates from LSH buckets for user {}", userId);
        similarityCandidateRepository.updateSimilarityCandidatesForUser(userId);

        log.info("Generated similarity candidates for user {}", userId);
    }

    /**
     * Get candidate user IDs for similarity computation.
     *
     * @param userId the user ID
     * @return list of candidate user IDs to compare against
     */
    public List<Integer> getCandidatesForUser(int userId) {
        log.debug("Fetching similarity candidates for user {}", userId);
        List<Integer> candidates = similarityCandidateRepository.getByUserId(userId);
        log.info("Found {} similarity candidates for user {}", candidates.size(), userId);
        return candidates;
    }
}
