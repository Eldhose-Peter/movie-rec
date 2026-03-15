package com.example.recommendation.service.recommendation;

import com.example.recommendation.model.BaseRatingEvent;
import com.example.recommendation.model.ImdbRatingEvent;
import com.example.recommendation.model.InternalRatingEvent;
import com.example.recommendation.repository.InternalRatingRepository;
import com.example.recommendation.repository.RatingRepository;
import com.example.recommendation.service.lsh.MinHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Service for computing and managing user MinHash signatures.
 * Signatures are computed from all movies a user has rated (both IMDB and
 * internal ratings).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserSignatureService {

        private final RatingRepository ratingRepository;
        private final InternalRatingRepository internalRatingRepository;
        private final MinHasher minHasher;

        /**
         * Compute MinHash signature for a user based on all their ratings.
         * Combines IMDB ratings and internal ratings into a single signature.
         *
         * @param raterId the user ID
         * @return MinHash signature (array of hash values)
         */
        public int[] computeUserSignature(Integer raterId) {
                log.debug("Computing signature for user {}", raterId);

                // Fetch ratings from both sources
                List<ImdbRatingEvent> imdbRatings = ratingRepository.findByRaterId(raterId);
                List<InternalRatingEvent> internalRatings = internalRatingRepository.findByRaterId(raterId);

                // Combine and extract movie IDs
                Set<Integer> movieIds = Stream.concat(
                                imdbRatings.stream(),
                                internalRatings.stream())
                                .map(BaseRatingEvent::getMovieId)
                                .collect(Collectors.toSet());

                log.debug("User {} has {} total rated movies", raterId, movieIds.size());

                // Compute MinHash signature
                int[] signature = minHasher.computeSignature(movieIds);

                log.info("Computed signature for user {}: {} hash values from {} movies",
                                raterId, signature.length, movieIds.size());

                return signature;
        }
}
