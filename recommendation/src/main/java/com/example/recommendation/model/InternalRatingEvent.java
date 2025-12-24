package com.example.recommendation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "internal_ratings")
@NoArgsConstructor
public class InternalRatingEvent extends BaseRatingEvent {
    public InternalRatingEvent(Integer raterId, Integer movieId, Double rating) {
        super(raterId, movieId, rating);
    }
}
