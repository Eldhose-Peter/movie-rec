package com.example.recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DatabaseDetails {
    private Long totalRatings;
    private Double averageRatingsPerUser;
    private Long totalMovies;
}
