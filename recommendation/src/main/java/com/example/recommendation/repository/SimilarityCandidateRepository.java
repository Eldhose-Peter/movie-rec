package com.example.recommendation.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SimilarityCandidateRepository {
    private final JdbcTemplate jdbcTemplate;

    public List<Integer> getByUserId(int userId) {
        String sql = "SELECT other_rater_id FROM similarity_candidate WHERE rater_id = ?";
        return jdbcTemplate.queryForList(sql, Integer.class, userId);
    }
    public void deleteSimilarityCandidatesByUserId(int userId) {
        String sql = "DELETE FROM similarity_candidate WHERE rater_id = ?";
        int rows = jdbcTemplate.update(sql, userId);

        // 3. Verify insertion
        System.out.println("DEBUG: Deleted " + rows + " rows.");
    }

    public void updateSimilarityCandidatesForUser(int userId) {
        String sql = """
            INSERT INTO similarity_candidate (rater_id, other_rater_id)
            SELECT DISTINCT a.rater_id, b.rater_id
            FROM lsh_bucket a
            JOIN lsh_bucket b 
              ON a.bucket_id = b.bucket_id 
            WHERE a.rater_id = ?
            AND b.rater_id != a.rater_id
        """;
        int rows = jdbcTemplate.update(sql, userId);

        // 3. Verify insertion
        System.out.println("DEBUG: Inserted " + rows + " rows.");
    }
}
