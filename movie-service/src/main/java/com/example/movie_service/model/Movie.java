package com.example.movie_service.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class Movie {

    private Integer Id;
    private String title;
    private String originalTitle;
    private LocalDate releaseDate;
    private String overview;
    private Double popularity;
    private Integer voteCount;
    private Double voteAverage;
    private String originalLanguage;
    private String backdropPath;
    private String posterPath;
    private Boolean adult;
    private Boolean video;
    private List<Integer> genreIds;
}
