package com.example.recommendation.repository;

import com.example.recommendation.model.InternalRatingEvent;
import org.springframework.data.repository.Repository;

public interface InternalRatingRepository extends Repository<InternalRatingEvent, Long> {
    InternalRatingEvent save(InternalRatingEvent ratingEvent);
}
