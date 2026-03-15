package com.example.recommendation.service.strategy;

import com.example.recommendation.model.ImdbRatingEvent;
import com.example.recommendation.model.SimilarItem;
import com.example.recommendation.model.UserSignature;
import com.example.recommendation.repository.RatingRepository;
import com.example.recommendation.service.lsh.CandidateGenerator;
import com.example.recommendation.service.lsh.LSHService;
import com.example.recommendation.service.lsh.MinHasher;
import com.example.recommendation.service.lsh.Similarity;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Strategy that uses LSH (Locality-Sensitive Hashing) with MinHash signatures.
 * Computes approximate similarities by only comparing candidate pairs.
 *
 * Pros:
 * - Dramatically reduces comparisons: instead of O(n²), only compares
 * candidates
 * - Scales well to large datasets (138k raters)
 * - Approximate results are usually very close to exact
 * - Good for finding similar users efficiently
 *
 * Cons:
 * - Approximate results (may miss some similar raters)
 * - Requires parameters tuning (num hashes, bands, rows per band)
 * - More complexity, harder to debug
 * - Incomplete: TODO in original code suggests storing results not implemented
 */
@Slf4j
public class LSHApproximateStrategy implements RaterSimilarityStrategy {

    private static final int DEFAULT_NUM_HASHES = 128;
    private static final int DEFAULT_BANDS = 32;
    private static final int DEFAULT_ROWS_PER_BAND = 4;

    private int numHashes;
    private int bands;
    private int rowsPerBand;

    public LSHApproximateStrategy() {
        this.numHashes = DEFAULT_NUM_HASHES;
        this.bands = DEFAULT_BANDS;
        this.rowsPerBand = DEFAULT_ROWS_PER_BAND;
    }

    public LSHApproximateStrategy(int numHashes, int bands, int rowsPerBand) {
        this.numHashes = numHashes;
        this.bands = bands;
        this.rowsPerBand = rowsPerBand;
    }

    public void setNumHashes(int numHashes) {
        this.numHashes = numHashes;
    }

    public void setBands(int bands) {
        this.bands = bands;
    }

    public void setRowsPerBand(int rowsPerBand) {
        this.rowsPerBand = rowsPerBand;
    }

    @Override
    public List<SimilarItem> compute(Integer currentRaterId,
            Map<Integer, Double> currentRatingsMap,
            RatingRepository repository) {
        log.info("Starting LSHApproximateStrategy for rater {} with {} hashes, {} bands, {} rows/band",
                currentRaterId, numHashes, bands, rowsPerBand);

        // Load all ratings
        List<ImdbRatingEvent> allRatings = repository.findAll();
        log.debug("Loaded {} total rating events from DB", allRatings.size());

        // Group by rater
        Map<Integer, List<ImdbRatingEvent>> ratingsByRater = allRatings.stream()
                .collect(Collectors.groupingBy(ImdbRatingEvent::getRaterId));

        log.debug("Grouped ratings by {} raters", ratingsByRater.size());

        // Step 1: Build MinHash signatures
        MinHasher minHasher = new MinHasher(numHashes);
        List<UserSignature> signatures = ratingsByRater.entrySet().stream()
                .map(entry -> {
                    int userId = entry.getKey();
                    Set<Integer> movies = entry.getValue().stream()
                            .map(ImdbRatingEvent::getMovieId)
                            .collect(Collectors.toSet());
                    int[] sig = minHasher.computeSignature(movies);
                    return new UserSignature(userId, sig);
                })
                .collect(Collectors.toList());

        log.info("Built {} user signatures", signatures.size());

        // Step 2: LSH Bucketing
        LSHService lshService = new LSHService(bands, rowsPerBand);
        Map<String, List<Integer>> buckets = lshService.bucketUsers(signatures);
        log.debug("Generated {} LSH buckets", buckets.size());

        // Step 3: Generate candidate pairs
        CandidateGenerator generator = new CandidateGenerator();
        Set<String> candidates = generator.generateCandidates(buckets);
        log.info("Generated {} candidate pairs for comparison", candidates.size());

        // Step 4: Compute exact similarities for candidates
        List<SimilarItem> similarities = new ArrayList<>();

        for (String pair : candidates) {
            String[] ids = pair.split("-");
            int u1 = Integer.parseInt(ids[0]);
            int u2 = Integer.parseInt(ids[1]);

            // Skip if doesn't involve current rater
            if (u1 != currentRaterId && u2 != currentRaterId) {
                continue;
            }

            // Determine other rater
            int otherRaterId = (u1 == currentRaterId) ? u2 : u1;

            List<ImdbRatingEvent> otherRatings = ratingsByRater.get(otherRaterId);
            if (otherRatings == null) {
                continue;
            }

            Map<Integer, Double> otherRatingMap = otherRatings.stream()
                    .collect(Collectors.toMap(ImdbRatingEvent::getMovieId, ImdbRatingEvent::getRating));

            // Compute cosine similarity for exact result
            double similarity = Similarity.cosine(currentRatingsMap, otherRatingMap);

            if (similarity > 0) {
                similarities.add(new SimilarItem(otherRaterId, similarity));
            }
        }

        // Sort by similarity descending
        similarities.sort(Comparator.comparing(SimilarItem::getSimilarity).reversed());

        log.info("LSHApproximateStrategy computed {} similarities from {} candidates",
                similarities.size(), candidates.size());
        return similarities;
    }
}
