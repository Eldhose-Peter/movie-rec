package com.example.recommendation.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_recommendations")
@Getter
@NoArgsConstructor
public class UserRecommendation {
    @EmbeddedId
    private UserRecommendationKey id;

    @Column(name = "weighted_sum_total")
    private double weightedSumTotal;

    @Column(name = "weight_total")
    private double weightTotal;

    public UserRecommendation(Long userId, Long movieId, double weightedSumTotal, double weightTotal) {
        this.id = new UserRecommendationKey(userId, movieId);
        this.weightedSumTotal = weightedSumTotal;
        this.weightTotal = weightTotal;
    }

    public Long getUserId() { return id != null ? id.getUserId() : null; }
    public Long getMovieId() { return id != null ? id.getMovieId() : null; }

}

