package com.example.recommendation.batch.step3;

import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GenerateCandidatePairsTasklet implements Tasklet {

    private final JdbcTemplate jdbcTemplate;

    public GenerateCandidatePairsTasklet(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {

        String sql = """
            INSERT INTO similarity_candidate (rater_id, other_rater_id)
            SELECT a.rater_id, b.rater_id
            FROM lsh_bucket a
            JOIN lsh_bucket b 
              ON a.bucket_id = b.bucket_id 
             AND a.rater_id < b.rater_id
            ON CONFLICT DO NOTHING
        """;

        int inserted = jdbcTemplate.update(sql);
        System.out.println("✅ Inserted " + inserted + " candidate pairs into similarity_candidate.");

        return RepeatStatus.FINISHED;
    }
}
