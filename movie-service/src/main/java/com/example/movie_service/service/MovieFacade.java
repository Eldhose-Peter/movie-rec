package com.example.movie_service.service;


import com.example.movie_service.Recommendation;
import com.example.movie_service.grpc.RecommendationGrpcClient;
import com.example.movie_service.model.Movie;
import com.example.movie_service.repository.MovieRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MovieFacade {

    private final MovieRepository repo;
    private final RecommendationGrpcClient client;
    private final MovieFilterService movieFilterService;

    public MovieFacade(MovieRepository repo, RecommendationGrpcClient client, MovieFilterService movieFilterService) {
        this.repo = repo;
        this.client = client;
        this.movieFilterService = movieFilterService;
    }

    public List<Movie> list(String search, Integer genreId, int page, int size, String sortBy, boolean desc) {
        int offset = Math.max(0, page) * size;
        return repo.findAll(search, genreId, offset, size, sortBy, desc);
    }

    public List<Movie> recommend(String userId, List<Integer> genreIds, Integer yearGte, Integer yearLte, Integer ratingGte, int page, int safeSize, String sort){

        // fetch recommendations from your client
        // TODO: page and limit needs to be implemented in rec service
        List<Recommendation> recommendations = client.getRecommendations(Long.parseLong(userId), 50, 0);
        if (recommendations == null || recommendations.isEmpty()) {
            return Collections.emptyList();
        }

        List<Integer> recIds = recommendations.stream()
                .map(Recommendation::getMovieId)   // assumed Long
                .map(Long::intValue)
                .collect(Collectors.toList());

        if (recIds.isEmpty()) {
            return Collections.emptyList();
        }

        // fetch movies data based on recId
        List<Movie> movies = repo.findByIds(recIds);

        Map<Integer, Movie> movieMap = movies.stream().collect(Collectors.toMap(Movie::getId, m -> m));

        // Build initial ordered list by recommendation order, skipping missing ids
        List<Movie> candidates = recIds.stream()
                .map(movieMap::get)
                .filter(Objects::nonNull)
                .toList();

        Predicate<Movie> filters = movieFilterService.buildFilter(genreIds, yearGte, yearLte, ratingGte);
        Comparator<Movie> sorter = movieFilterService.resolveComparator(sort);

        return candidates.stream().filter(filters).sorted(sorter).skip((long) page * safeSize).limit(safeSize).collect(Collectors.toList());
    }

}
