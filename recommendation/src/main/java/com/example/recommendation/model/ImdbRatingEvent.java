package com.example.recommendation.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ratings")
public class ImdbRatingEvent extends BaseRatingEvent{
    public ImdbRatingEvent(Integer raterId, Integer movieId, Double rating) {
        super(raterId, movieId, rating);
    }
}
