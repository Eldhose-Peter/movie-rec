package com.example.recommendation.service;

import com.example.recommendation.model.DatabaseDetails;
import com.example.recommendation.model.RatingEvent;
import com.example.recommendation.repository.RatingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RatingTestService {
    private final RatingRepository ratingRepository;

    public RatingTestService(RatingRepository ratingRepository){
        this.ratingRepository = ratingRepository;
    }

    public List<Integer> getAllRaterIds() {
        return ratingRepository.getUniqueRaterIds();
    }

    public List<RatingEvent> getRatingsByUser(Integer raterId) {
        return ratingRepository.findById_RaterId(raterId);
    }

    public RatingEvent getRatingForMovieByUser(Integer movieId, Integer raterId) {
        return ratingRepository.findById_RaterIdAndId_MovieId(raterId, movieId);
    }

    public DatabaseDetails getDetails() {
        Long totalRatings = ratingRepository.getTotalRatings();
        Long totalUsers = ratingRepository.getTotalUsers();
        Long totalMovies = ratingRepository.getTotalMovies();

        double avgRatingsPerUser = 0.0;
        if (totalUsers != null && totalUsers > 0) {
            avgRatingsPerUser = (double) totalRatings / totalUsers;
        }

        return new DatabaseDetails(totalRatings, avgRatingsPerUser, totalMovies);
    }
}
