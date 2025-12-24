package com.example.recommendation.repository;

import com.example.recommendation.model.InternalRatingEvent;

public interface InternalRatingRepository {
    InternalRatingEvent save(InternalRatingEvent ratingEvent);
}
