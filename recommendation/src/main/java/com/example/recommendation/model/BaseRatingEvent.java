package com.example.recommendation.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public class BaseRatingEvent {
    @EmbeddedId
    private RatingId id;
    private Double rating;
    @Column(name = "time")
    private LocalDateTime timestamp;

    public BaseRatingEvent(Integer raterId, Integer movieId, Double rating){
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
