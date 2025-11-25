package com.example.movie_service.service;

import com.example.movie_service.model.Movie;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MovieFilterService {
    public Predicate<Movie> buildFilter(List<Integer> genreIds, Integer yearGte, Integer yearLte, Integer ratingGte) {
        return filterByGenre(parseGenreIds(genreIds))
                .and(filterByYearRange(yearGte, yearLte))
                .and(filterByRating(ratingGte));
    }

    public Comparator<Movie> resolveComparator(String sort) {
        log.info("Received sort request for value: '{}'", sort);

        if (sort == null || sort.isBlank() || "recommendation".equalsIgnoreCase(sort)) {
            log.info("Sort is null/default. Keeping original recommendation order.");
            return (m1, m2) -> 0;
        }

        switch (sort.toLowerCase(Locale.ROOT)) {
            case "rating_desc": {
                log.info("Applying sort: Rating (High to Low)");
                return Comparator.comparingDouble(Movie::getVoteAverage).reversed();
            }
            case "rating_asc": {
                log.info("Applying sort: Rating (Low to High)");
                return Comparator.comparingDouble(Movie::getVoteAverage);
            }
            case "year_desc": {
                log.info("Applying sort: Release Year (Newest First)");
                return Comparator.comparingInt(this::getYearSafe).reversed();
            }
            case "year_asc": {
                log.info("Applying sort: Release Year (Oldest First)");
                return Comparator.comparingInt(this::getYearSafe);
            }
            default: {
                log.warn("Sort parameter '{}' is not recognized. Returning unsorted list.", sort);
                return (m1, m2) -> 0;
            }
        }    }


    private int getYearSafe(Movie m) {
        return (m.getReleaseDate() == null) ? 0 : m.getReleaseDate().getYear();
    }

    private Predicate<Movie> filterByGenre(Set<Integer> genreIds) {
        return movie -> {
            if (genreIds == null || genreIds.isEmpty()) return true;
            if (movie.getGenreIds() == null) return false;
            // True if movie has ANY of the requested genres
            return movie.getGenreIds().stream().anyMatch(genreIds::contains);
        };
    }

    private Predicate<Movie> filterByYearRange(Integer min, Integer max) {
        return movie -> {
            if (movie.getReleaseDate() == null) return false;
            int year = movie.getReleaseDate().getYear();
            boolean afterMin = (min == null) || (year >= min);
            boolean beforeMax = (max == null) || (year <= max);
            return afterMin && beforeMax;
        };
    }

    private Predicate<Movie> filterByRating(Integer minRating) {
        return movie -> (minRating == null) || (movie.getVoteAverage() >= minRating);
    }

    private Set<Integer> parseGenreIds(List<Integer> genreIds) {
        if (genreIds == null) return Collections.emptySet();
        return genreIds.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
