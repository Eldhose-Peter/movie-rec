package com.example.recommendation.service;

import com.example.recommendation.model.ImdbRatingEvent;
import com.example.recommendation.model.SimilarItem;
import com.example.recommendation.repository.RatingRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class WeightedRatingService {

    RaterSimilarityService raterSimilarityService;
    RatingRepository ratingRepository;

    WeightedRatingService(RatingRepository ratingRepository, RaterSimilarityService raterSimilarityService){
        this.ratingRepository = ratingRepository;
        this.raterSimilarityService = raterSimilarityService;
    }

    public List<SimilarItem> getSimilarMovies(Integer currentRaterId, int numSimilarRaters, int minimalRaters){
        return this.calculateSimilarMovies(currentRaterId,numSimilarRaters,minimalRaters);

    }

    private List<SimilarItem> calculateSimilarMovies(Integer currentRaterId, int numSimilarRaters, int minimalRaters){
        List<SimilarItem> similarRaters = this.raterSimilarityService.getSimilarRaters(currentRaterId);
        List<Integer> uniqueMovieIds = this.ratingRepository.getUniqueMovieIds();

        List<SimilarItem> similarMovies = new ArrayList<>();

        for(Integer movieId: uniqueMovieIds){
            List<Double> weightedRatingsList = this.calculateWeightedRatings(movieId,similarRaters, numSimilarRaters);

            if(weightedRatingsList.size() > minimalRaters){
                // Sum all weighted ratings
                double sum = weightedRatingsList.stream()
                        .mapToDouble(Double::doubleValue)
                        .sum();

                // Average
                double average = sum / weightedRatingsList.size();

                // Create Similarity object
                SimilarItem movieSimilarity = new SimilarItem(movieId, average);

                // Add to similar movies list
                similarMovies.add(movieSimilarity);
            }

        }

        similarMovies.sort(SimilarItem::compareTo);

        return similarMovies;

    }

    private List<Double> calculateWeightedRatings(Integer movieId, List<SimilarItem> similarRaters, int numSimilarRaters) {
        List<Double> weightedRatingsList = new ArrayList<>();

        for (int i = 0; i < numSimilarRaters && i < similarRaters.size(); i++) {
            SimilarItem similarRater = similarRaters.get(i);
            Integer raterId = similarRater.getItemId();
            double raterCloseness = similarRater.getSimilarity();

            if (raterCloseness <= 0) {
                break;
            }


            ImdbRatingEvent movieRatings = this.ratingRepository.findById_RaterIdAndId_MovieId(movieId, raterId);

            if (movieRatings!=null) {
                double weightedRating = raterCloseness * movieRatings.getRating();
                weightedRatingsList.add(weightedRating);
            }
        }

        return weightedRatingsList;
    }
}
