package com.example.recommendation.service.lsh;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Random;
import java.util.Set;

@Component
public class MinHasher {
    private final int numHashes;
    private final int[] a, b;
    private final int prime = 10000019; // large prime > totalMovies

    public MinHasher(@Value("${minhash.numHashes:128}") int numHashes) {
        this.numHashes = numHashes;
        Random rand = new Random(42);
        a = new int[numHashes];
        b = new int[numHashes];
        for (int i = 0; i < numHashes; i++) {
            a[i] = rand.nextInt(prime - 1) + 1;
            b[i] = rand.nextInt(prime - 1);
        }
    }

    public int[] computeSignature(Set<Integer> movieIds) {
        int[] sig = new int[numHashes];
        Arrays.fill(sig, Integer.MAX_VALUE);

        for (int movieId : movieIds) {
            for (int i = 0; i < numHashes; i++) {
                int hash = (int)((((long)a[i] * movieId + b[i]) % prime));
                sig[i] = Math.min(sig[i], hash);
            }
        }
        return sig;
    }
}
