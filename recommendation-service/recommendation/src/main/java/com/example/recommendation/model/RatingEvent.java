package com.example.recommendation.model;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ratings")
@Getter
public class RatingEvent {
    @EmbeddedId
    private RatingId id;
    private Double rating;
    @Column(name = "time")
    private LocalDateTime timestamp;

    // Convenience getters
    @Transient
    public Integer getMovieId() {
        return id != null ? id.getMovieId() : null;
    }

    @Transient
    public Integer getRaterId() {
        return id != null ? id.getRaterId() : null;
    }

}
