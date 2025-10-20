package com.example.recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieWeightContribution {
    private int userId;        // rater_id
    private int movieId;
    private double weightedSum;
    private double weightSum;
}

