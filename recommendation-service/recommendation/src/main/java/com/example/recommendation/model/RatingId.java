package com.example.recommendation.model;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

public class RatingId implements Serializable {
    @Getter
    @Setter
    private Integer raterId;

    @Getter
    @Setter
    private Integer movieId;


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RatingId that)) return false;
        return Objects.equals(raterId, that.raterId) &&
                Objects.equals(movieId, that.movieId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(raterId, movieId);
    }
}
