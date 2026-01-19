package com.example.recommendation.repository;

import com.example.recommendation.model.InternalRatingEvent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface InternalRatingRepository extends Repository<InternalRatingEvent, Long> {
    InternalRatingEvent save(InternalRatingEvent ratingEvent);

    @Query("SELECT r FROM InternalRatingEvent r WHERE r.id.raterId = :raterId")
    List<InternalRatingEvent> findByRaterId(Integer raterId);
}
