package com.example.recommendation.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SimilarityCandidateRepository {
    private final JdbcTemplate jdbcTemplate = null;

    public List<Integer> getByUserId(int userId) {
        String sql = "SELECT other_rater_id FROM similarity_candidates WHERE rater_id = ?";
        return jdbcTemplate.queryForList(sql, Integer.class, userId);
    }
    public void deleteSimilarityCandidatesByUserId(int userId) {
        String sql = "DELETE FROM similarity_candidates WHERE user_id = ?";
        jdbcTemplate.update(sql, userId);
    }

    public void updateSimilarityCandidatesForUser(int userId) {
        String sql = """
            INSERT INTO similarity_candidates (rater_id, other_rater_id)
            SELECT a.user_id, b.user_id
            FROM lsh_buckets a
            JOIN lsh_buckets b 
              ON a.bucket_id = b.bucket_id 
             AND a.user_id < b.user_id
            WHERE a.user_id = ?
            ON CONFLICT DO NOTHING
        """;
        jdbcTemplate.update(sql, userId);
    }
}
