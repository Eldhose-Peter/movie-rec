package com.example.recommendation.service;

import com.example.recommendation.model.*;
import com.example.recommendation.repository.*;
import com.example.recommendation.service.lsh.LSHService;
import com.example.recommendation.service.lsh.MinHasher;
import com.example.recommendation.service.lsh.Similarity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class IncrementalRecService {

    private final InternalRatingRepository internalRatingRepository;
    private final LSHBucketRepository lshBucketRepository;
    private final SimilarityCandidateRepository similarityCandidateRepository;
    private final UserSimilarityRepository userSimilarityRepository;
    private final RatingRepository ratingRepository;
    private final MinHasher minHasher;
    private final LSHService lshService;
    private final UserRecommendationRepository userRecommendationRepository;

    @Transactional
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
        log.info("Computed new signature for user {}: {} : {}", raterId, signature,movieIds.size() );
        return signature;

    }

    private void findNewBucketsForUser(Integer raterId, int[] newSignature) {
        List<LSHService.BucketEntry> bucketEntries = lshService.computeBuckets(raterId, newSignature);
        log.info("Computed new buckets for user {}: {}", raterId, bucketEntries);
        // remove old buckets and save new buckets to DB
        lshBucketRepository.deleteByRaterId(raterId);
        List<LSHBucket> newBuckets = new ArrayList<>();
        for(LSHService.BucketEntry entry : bucketEntries) {
            newBuckets.add(new LSHBucket(entry.bucketId(), entry.userId()));
        }

        // SAVE AND FLUSH: Forces the data to the DB immediately
        lshBucketRepository.saveAll(newBuckets); // batch save is faster
        lshBucketRepository.flush();

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
        List<InternalRatingEvent> ratings1 = internalRatingRepository.findByRaterId(userId);

        Map<Integer, Double> r1 = new HashMap<>();
        for (InternalRatingEvent r : ratings1) r1.put(r.getMovieId(), r.getRating());

        for (Integer r2Id : candidates)
        {
            List<ImdbRatingEvent> ratings2 = ratingRepository.findById_RaterId(r2Id);

            log.info("Computing similarity between user {} and {} with {} and {} ratings", userId, r2Id, ratings1.size(), ratings2.size());
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

    @Transactional
    public void refreshRecommendations(int userId) {
        log.info("Refreshing recommendations for user {}", userId);

        // 1. Fetch similar users
        List<UserSimilarity> similarities = userSimilarityRepository.findByIdRaterIdOrderBySimilarityScoreDesc(userId);
        log.info("Found {} similar users for user {}", similarities.size(), userId);
        if (similarities.isEmpty()) return;

        Map<Integer, Double> simMap = similarities.stream()
                .collect(Collectors.toMap(s -> s.getId().getOtherRaterId(), UserSimilarity::getSimilarityScore));

        // 2. Fetch target user's rated movies to exclude them
        Set<Integer> seenMovies = internalRatingRepository.findByRaterId(userId).stream()
                .map(InternalRatingEvent::getMovieId)
                .collect(Collectors.toSet());

        // 3. Batch fetch ALL ratings from ALL similar users in one go
        List<ImdbRatingEvent> allSimilarRatings = ratingRepository.findByRaterIds(simMap.keySet());

        // 4. Aggregate scores in-memory using a custom accumulator
        Map<Integer, Double[]> aggregationMap = new HashMap<>(); // MovieId -> [WeightedSum, WeightSum]

        for (ImdbRatingEvent rating : allSimilarRatings) {
            if (seenMovies.contains(rating.getMovieId())) continue;

            log.info("Processing rating from user {} for movie {}: {}", rating.getRaterId(), rating.getMovieId(), rating.getRating());

            double simScore = simMap.get(rating.getRaterId());
            Double[] totals = aggregationMap.computeIfAbsent(rating.getMovieId(), k -> new Double[]{0.0, 0.0});

            totals[0] += rating.getRating() * simScore; // Weighted Sum
            totals[1] += simScore;                      // Sum of Weights

            log.info("Updated aggregation for movie {}: WeightedSum={}, WeightSum={}", rating.getMovieId(), totals[0], totals[1]);
        }

        log.info("Aggregated scores for {} candidate movies for user {}", aggregationMap.size(), userId);

        // 5. Transform to recommendation entities
        List<UserRecommendation> recommendations = aggregationMap.entrySet().stream()
                .map(entry -> {
                    return new UserRecommendation((long) userId, (long) entry.getKey(),entry.getValue()[0] , entry.getValue()[1]);
                })
                .limit(20)
                .collect(Collectors.toList());

        log.info("Generated top {} recommendations for user {}", recommendations.size(), userId);

        // 6. Atomic Update
        userRecommendationRepository.deleteByIdUserId(userId);
        userRecommendationRepository.saveAll(recommendations);

        log.info("Refreshed recommendations saved for user {}", userId);
    }

}
