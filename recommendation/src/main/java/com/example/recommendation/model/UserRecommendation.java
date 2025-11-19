package com.example.recommendation.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "user_recommendations")
@Getter
public class UserRecommendation {
    @EmbeddedId
    private UserRecommendationKey id;

    @Column(name = "weighted_sum_total")
    private double weightedSumTotal;

    @Column(name = "weight_total")
    private double weightTotal;

    public Long getUserId() { return id != null ? id.getUserId() : null; }
    public Long getMovieId() { return id != null ? id.getMovieId() : null; }

}

