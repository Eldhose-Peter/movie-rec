package com.example.recommendation.service;

import com.example.recommendation.model.*;
import com.example.recommendation.repository.*;
import com.example.recommendation.service.lsh.LSHService;
import com.example.recommendation.service.lsh.MinHasher;
import com.example.recommendation.service.lsh.Similarity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncrementalRecService {

    InternalRatingRepository internalRatingRepository;
    LSHBucketRepository lshBucketRepository;
    SimilarityCandidateRepository similarityCandidateRepository;
    UserSimilarityRepository userSimilarityRepository;
    RatingRepository ratingRepository;
    MinHasher minHasher;
    LSHService lshService;

    public void processNewRating(InternalRatingEvent rating) {
        log.info("Processing new rating: {}", rating);
        saveRating(rating);
        log.info("Saved new rating to internal repository");
        int[] newSignature = updateSignatureForUser(rating.getRaterId());
        log.info("Updated signature for user {}", rating.getRaterId());
        findNewBucketsForUser(rating.getRaterId(), newSignature);
        log.info("Updated LSH buckets for user {}", rating.getRaterId());
        generateCandidatesFromBuckets(rating.getRaterId());
        log.info("Generated similarity candidates for user {}", rating.getRaterId());
        computeSimilarities(rating.getRaterId());
        log.info("Computed user similarities for user {}", rating.getRaterId());
        refreshRecommendations(rating.getRaterId());
        log.info("Refreshed recommendations for user {}", rating.getRaterId());
    }

    private void saveRating(InternalRatingEvent rating) {
        internalRatingRepository.save(rating);
    }

    private int[] updateSignatureForUser(Integer raterId) {
        List<ImdbRatingEvent> imdbRatingEventList = ratingRepository.findByRaterId(raterId);
        List<InternalRatingEvent> internalRatingEventList = internalRatingRepository.findByRaterId(raterId);

        List<BaseRatingEvent> ratings = Stream.concat(
            imdbRatingEventList.stream(),
            internalRatingEventList.stream()
        ).toList();

        Set<Integer> movieIds = ratings.stream()
                .map(BaseRatingEvent::getMovieId)
                .collect(Collectors.toSet());

        int[] signature = minHasher.computeSignature(movieIds);
        log.info("Computed new signature for user {}: {}", raterId, signature);
        return signature;

    }

    private void findNewBucketsForUser(Integer raterId, int[] newSignature) {
        List<LSHService.BucketEntry> bucketEntries = lshService.computeBuckets(raterId, newSignature);
        LSHBucket bucket =  new LSHBucket(bucketEntries.getFirst().bucketId(), bucketEntries.getFirst().userId());
        log.info("Computed new buckets for user {}: {}", raterId, bucketEntries);
        // remove old buckets and save new buckets to DB
        lshBucketRepository.deleteByRaterId(raterId);
        lshBucketRepository.save(bucket);

    }

    private void generateCandidatesFromBuckets( int userId ) {
        // Implementation here
        // remove old candidates for user from DB
        similarityCandidateRepository.deleteSimilarityCandidatesByUserId(userId);
        // fetch buckets for user from DB and generate candidates and save to DB
        similarityCandidateRepository.updateSimilarityCandidatesForUser(userId);

    }

    private void computeSimilarities(int userId) {
        // Implementation here
        // remove old similarities for user from DB
        userSimilarityRepository.deleteByRaterId(userId);
        // fetch candidates for user from similarity DB
        List<Integer> candidates = similarityCandidateRepository.getByUserId(userId);
        log.info("Found {} similarity candidates for user {}", candidates.size(), userId);

        // compute similarities and save to DB
        List<ImdbRatingEvent> ratings1 = ratingRepository.findById_RaterId(userId);

        Map<Integer, Double> r1 = new HashMap<>();
        for (ImdbRatingEvent r : ratings1) r1.put(r.getMovieId(), r.getRating());

        for (Integer r2Id : candidates)
        {
            List<ImdbRatingEvent> ratings2 = ratingRepository.findById_RaterId(r2Id);
            // compute similarity between ratings1 and ratings2
            double sim = Similarity.cosine(
                r1,
                ratings2.stream().collect(Collectors.toMap(BaseRatingEvent::getMovieId, BaseRatingEvent::getRating))
            );

            log.info("Computed similarity between user {} and {}: {}", userId, r2Id, sim);

            // save similarity to DB
            UserSimilarity similarity = new UserSimilarity(userId, r2Id, sim);
            userSimilarityRepository.save(similarity);
        }
    }

    private void refreshRecommendations(int userId) {
        log.info("Refreshing recommendations for user {}", userId);
        // Implementation here
        // TODO: remove old recommendations for user from DB
        // TODO: fetch similarities for user from DB
        // TODO: generate new recommendations and save to DB
    }

}
