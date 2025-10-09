package com.example.recommendation.service.lsh;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Similarity {
    public static double cosine(Map<Integer, Double> ratings1, Map<Integer, Double> ratings2) {
        double dot = 0, norm1 = 0, norm2 = 0;
        for (Map.Entry<Integer, Double> e : ratings1.entrySet()) {
            int movie = e.getKey();
            double r1 = e.getValue();
            norm1 += r1 * r1;
            if (ratings2.containsKey(movie)) {
                double r2 = ratings2.get(movie);
                dot += r1 * r2;
            }
        }
        for (double r2 : ratings2.values()) {
            norm2 += r2 * r2;
        }
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2) + 1e-9);
    }

    public static double dotProduct(Map<Integer, Double> ratings1, Map<Integer, Double> ratings2) {
        double dot = 0.0;
        for (Map.Entry<Integer, Double> e : ratings1.entrySet()) {
            int movie = e.getKey();
            double r1 = e.getValue() -5;
            if (ratings2.containsKey(movie)) {
                double r2 = ratings2.get(movie) -5;
                dot += r1 * r2;
            }
        }
        return dot;
    }
}

