package com.example.movie_service.service;


import com.example.movie_service.model.Movie;
import com.example.movie_service.repository.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieFacade {

    private final MovieRepository repo;

    public MovieFacade(MovieRepository repo) {
        this.repo = repo;
    }

    public List<Movie> list(String search, Integer genreId, int page, int size, String sortBy, boolean desc) {
        int offset = Math.max(0, page) * size;
        return repo.findAll(search, genreId, offset, size, sortBy, desc);
    }

    public List<Movie> recommend(List<String> genreIds, Integer yearGte, Integer yearLte, Integer ratingGte, int page, int safeSize, String sort){

        return List.of();
    }

}
