package com.example.recommendation.batch.step4;

import com.example.recommendation.model.UserSimilarity;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class UserSimilarityWriter {
    @Bean
    @StepScope
    public JdbcBatchItemWriter<UserSimilarity> similarityWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<UserSimilarity>()
                .dataSource(dataSource)
                .sql("""
            INSERT INTO user_similarity (rater_id, other_rater_id, similarity_score)
            VALUES (:raterId, :otherRaterId, :similarityScore)
            ON CONFLICT (rater_id, other_rater_id)
            DO UPDATE SET similarity_score = EXCLUDED.similarity_score
        """)
                .beanMapped()
                .build();
    }

}
