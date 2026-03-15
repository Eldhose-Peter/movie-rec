package com.example.recommendation.service.recommendation;

import com.example.recommendation.model.LSHBucket;
import com.example.recommendation.repository.LSHBucketRepository;
import com.example.recommendation.service.lsh.LSHService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for managing LSH (Locality-Sensitive Hashing) bucket computations.
 * Stores which buckets a user belongs to based on their signature.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LSHBucketService {

    private final LSHBucketRepository lshBucketRepository;
    private final LSHService lshService;

    /**
     * Update LSH buckets for a user.
     * Deletes old buckets and saves new ones computed from the signature.
     *
     * @param raterId   the user ID
     * @param signature the MinHash signature of the user
     */
    public void updateUserBuckets(Integer raterId, int[] signature) {
        log.debug("Updating LSH buckets for user {} with signature of length {}",
                raterId, signature.length);

        // Compute new bucket entries
        List<LSHService.BucketEntry> bucketEntries = lshService.computeBuckets(raterId, signature);
        log.debug("Computed {} new buckets for user {}", bucketEntries.size(), raterId);

        // Delete old buckets
        log.debug("Deleting old buckets for user {}", raterId);
        lshBucketRepository.deleteByRaterId(raterId);

        // Create bucket entities and save
        List<LSHBucket> newBuckets = new ArrayList<>();
        for (LSHService.BucketEntry entry : bucketEntries) {
            newBuckets.add(new LSHBucket(entry.bucketId(), entry.userId()));
        }

        // Batch save for efficiency
        lshBucketRepository.saveAll(newBuckets);
        lshBucketRepository.flush(); // Force flush to ensure data is persisted immediately

        log.info("Updated LSH buckets for user {}: saved {} bucket entries",
                raterId, newBuckets.size());
    }
}
