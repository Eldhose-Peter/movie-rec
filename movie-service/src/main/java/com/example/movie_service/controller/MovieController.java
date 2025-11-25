package com.example.movie_service.controller;

import com.example.movie_service.model.Movie;
import com.example.movie_service.service.MovieFacade;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MovieController {

    private final MovieFacade movieFacade;

    public MovieController(MovieFacade movieFacade){
        this.movieFacade = movieFacade;
    }

    /**
     * GET /api/movies
     *
     * Query params:
     * - page (0-based) default 0
     * - size default 20
     * - search optional title/original_title substring (case-insensitive)
     * - genreId optional numeric genre id
     * - sort optional column (popularity|vote_average|release_date|title). default "id"
     * - desc optional boolean (true for descending)
     */
    @GetMapping("/movies")
    public ResponseEntity<List<Movie>> listMovies(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "genreId", required = false) Integer genreId,
            @RequestParam(name = "sort", defaultValue = "id") String sort,
            @RequestParam(name = "desc", defaultValue = "true") boolean desc
    ) {
        int safeSize = Math.min(Math.max(1, size), 200);
        List<Movie> list = movieFacade.list(search, genreId, page, safeSize, sort, desc);
        return ResponseEntity.ok(list);
    }


    @GetMapping("/users/{userId}/recommend")
    public ResponseEntity<List<Movie>> getRecommendedMovies(
            @PathVariable String userId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @RequestParam(name = "genre", required = false) List<Integer> genreIds,
            @RequestParam(name = "yearGte", required = false) Integer yearGte,
            @RequestParam(name = "yearLte", required = false) Integer yearLte,
            @RequestParam(name = "ratingGte", required = false) Integer ratingGte,
            @RequestParam(name = "sort", defaultValue = "id") String sort

    ) {
        int safeSize = Math.min(Math.max(1, size), 200);
        List<Movie> list = movieFacade.recommend(userId, genreIds, yearGte, yearLte, ratingGte, page, safeSize, sort);
        return ResponseEntity.ok(list);
    }

}
