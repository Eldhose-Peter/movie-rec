package com.example.recommendation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "internal_ratings")
public class InternalRatingEvent extends BaseRatingEvent {
    public InternalRatingEvent(Integer raterId, Integer movieId, Double rating) {
        super(raterId, movieId, rating);
    }
}
