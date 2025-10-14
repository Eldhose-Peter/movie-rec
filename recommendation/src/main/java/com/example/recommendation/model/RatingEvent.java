package com.example.recommendation.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ratings")
@Getter
public class RatingEvent {
    @EmbeddedId
    private final RatingId id;
    private final Double rating;
    @Column(name = "time")
    private LocalDateTime timestamp;

    public RatingEvent(Integer raterId, Integer movieId, Double rating){
        this.id = new RatingId(raterId,movieId);
        this.rating = rating;
    }

    // Convenience getters
    @Transient
    public Integer getMovieId() {
        return id.getMovieId();
    }

    @Transient
    public Integer getRaterId() {
        return id.getRaterId();
    }

}
