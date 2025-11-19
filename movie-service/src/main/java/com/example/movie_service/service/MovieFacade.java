package com.example.movie_service.service;


import com.example.movie_service.Recommendation;
import com.example.movie_service.RecommendationServiceGrpc;
import com.example.movie_service.grpc.RecommendationGrpcClient;
import com.example.movie_service.model.Movie;
import com.example.movie_service.repository.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MovieFacade {

    private final MovieRepository repo;
    private final RecommendationGrpcClient client;

    public MovieFacade(MovieRepository repo, RecommendationGrpcClient client) {
        this.repo = repo;
        this.client = client;
    }

    public List<Movie> list(String search, Integer genreId, int page, int size, String sortBy, boolean desc) {
        int offset = Math.max(0, page) * size;
        return repo.findAll(search, genreId, offset, size, sortBy, desc);
    }

    public List<Movie> recommend(String userId, List<String> genreIds, Integer yearGte, Integer yearLte, Integer ratingGte, int page, int safeSize, String sort){
        List<Recommendation> recommendations = client.getRecommendations(Long.parseLong(userId),page,safeSize);
        log.info("Received recommendations of size {}", recommendations.size());
        return List.of();
    }

}
