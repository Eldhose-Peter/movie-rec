package com.example.recommendation.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

@Setter
@Getter
@Embeddable
public class UserSimilarityKey implements Serializable {
    // getters & setters
    private int raterId;
    private int otherRaterId;

    public UserSimilarityKey() {}

    public UserSimilarityKey(int raterId, int otherRaterId) {
        this.raterId = raterId;
        this.otherRaterId = otherRaterId;
    }

    // equals and hashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserSimilarityKey)) return false;
        UserSimilarityKey that = (UserSimilarityKey) o;
        return raterId == that.raterId && otherRaterId == that.otherRaterId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(raterId, otherRaterId);
    }
}

