package com.example.recommendation.repository;

import com.example.recommendation.model.RatingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@RequiredArgsConstructor
public class RatingJdbcRepository {
    private final JdbcTemplate jdbcTemplate;

    public Map<Integer, List<RatingEvent>> getRatingsForRaters(Set<Integer> raterIds) {
        String sql = "SELECT rater_id, movie_id, rating FROM ratings WHERE rater_id = ANY (?)";
        return jdbcTemplate.query(sql, ps -> ps.setArray(1, ps.getConnection().createArrayOf("INTEGER", raterIds.toArray())),
                rs -> {
                    Map<Integer, List<RatingEvent>> map = new HashMap<>();
                    while (rs.next()) {
                        int raterId = rs.getInt("rater_id");
                        map.computeIfAbsent(raterId, k -> new ArrayList<>())
                                .add(new RatingEvent(raterId, rs.getInt("movie_id"), rs.getDouble("rating")));
                    }
                    return map;
                });
    }
}

