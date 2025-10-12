package com.example.recommendation.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(name = "user_similarity")
public class UserSimilarity {

    @EmbeddedId
    private UserSimilarityKey id;
    double similarityScore;

    public UserSimilarity(int raterId, int otherRaterId, double sim) {
        this.similarityScore = sim;
        this.id = new UserSimilarityKey(raterId,otherRaterId);
    }

    public int getRaterId() { return id.getRaterId(); }
    public int getOtherRaterId() { return id.getOtherRaterId(); }
}
