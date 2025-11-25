package com.example.recommendation.service;

import com.example.recommendation.model.UserRecommendation;
import com.example.recommendation.repository.UserRecommendationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserRecommendationService {

    private final UserRecommendationRepository repository;

    public UserRecommendationService(UserRecommendationRepository repository) {
        this.repository = repository;
    }

    // TODO : Implement limt and offset for pagination
    public List<UserRecommendation> getRecommendations(long userId, int limit, int offset) {
        return repository.findTopByUser(userId);
    }

}
