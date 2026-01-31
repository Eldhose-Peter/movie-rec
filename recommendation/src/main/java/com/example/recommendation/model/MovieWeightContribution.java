package com.example.recommendation.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class MovieWeightContribution {
    private int userId;        // rater_id
    private int movieId;
    private double weightedSum;
    private double weightSum;
}

