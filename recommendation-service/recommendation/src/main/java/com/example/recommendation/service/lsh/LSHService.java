package com.example.recommendation.service.lsh;

import com.example.recommendation.model.UserSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class LSHService {
    private final int bands;
    private final int rowsPerBand;

    public LSHService(@Value("${lsh.bands:32}") int bands, @Value("${lsh.rowsPerBand:4}") int rowsPerBand) {
        this.bands = bands;
        this.rowsPerBand = rowsPerBand;
    }

    public Map<String, List<Integer>> bucketUsers(List<UserSignature> signatures) {
        Map<String, List<Integer>> buckets = new HashMap<>();

        for (UserSignature sig : signatures) {
            int[] vector = sig.getSignature();
            for (int b = 0; b < bands; b++) {
                int start = b * rowsPerBand;
                int end = start + rowsPerBand;
                String bandKey = Arrays.toString(Arrays.copyOfRange(vector, start, end));

                buckets.computeIfAbsent(bandKey, k -> new ArrayList<>()).add(sig.getRaterId());
            }
        }
        return buckets;
    }

    public List<BucketEntry> computeBuckets(int userId, int[] signature) {
        List<BucketEntry> result = new ArrayList<>();
        for (int band = 0; band < bands; band++) {
            int start = band * rowsPerBand;
            int end = Math.min(start + rowsPerBand, signature.length);
            long hash = 1469598103934665603L;
            for (int i = start; i < end; i++) {
                hash ^= Integer.toUnsignedLong(signature[i]);
                hash *= 1099511628211L;
            }
            long bucketId = Math.abs(hash);
            result.add(new BucketEntry(bucketId, userId));
        }
        return result;
    }

    public record BucketEntry(long bucketId, int userId) {
    }
}
