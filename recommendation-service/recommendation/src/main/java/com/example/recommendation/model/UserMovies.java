package com.example.recommendation.model;

import java.util.Set;

public record UserMovies(int userId, Set<Integer> movies) {
}
