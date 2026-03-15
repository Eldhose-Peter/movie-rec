package com.example.recommendation.service.recommendation;

import com.example.recommendation.model.InternalRatingEvent;
import com.example.recommendation.repository.InternalRatingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service for persisting internal ratings (user-provided ratings).
 * Handles storage of incremental/real-time ratings as they come in.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RatingPersistenceService {

    private final InternalRatingRepository internalRatingRepository;

    /**
     * Save a new internal rating to the database.
     *
     * @param rating the rating event to persist
     */
    public void saveInternalRating(InternalRatingEvent rating) {
        log.debug("Saving internal rating: raterId={}, movieId={}, rating={}",
                rating.getRaterId(), rating.getMovieId(), rating.getRating());
        internalRatingRepository.save(rating);
        log.info("Saved internal rating for user {}", rating.getRaterId());
    }
}
