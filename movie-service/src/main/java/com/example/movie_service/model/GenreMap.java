package com.example.movie_service.model;

import lombok.Getter;

@Getter
public class GenreMap {
    private final Integer genreId;
    private final String genreName;

    public GenreMap(Integer genreId, String genreName) {
        this.genreId = genreId;
        this.genreName = genreName;
    }
}
