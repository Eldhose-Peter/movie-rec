package com.example.recommendation.batch.step5;


import com.example.recommendation.model.UserSimilarity;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;


public class UserSimilarityReader {

    @Bean
    @StepScope
    public JdbcCursorItemReader<UserSimilarity> userSimilarityCursorReader(DataSource dataSource){
        return new JdbcCursorItemReaderBuilder<UserSimilarity>()
                .name("userSimilarityReader")
                .dataSource(dataSource)
                .sql("""
                        SELECT *
                        FROM (
                            SELECT
                                rater_id,
                                other_rater_id,
                                similarity_score,
                                ROW_NUMBER() OVER (PARTITION BY rater_id ORDER BY similarity_score DESC) AS rn
                            FROM user_similarities
                        ) t
                        WHERE rn <= 50
                        ORDER BY rater_id, similarity_score DESC;
                    """)
                .rowMapper((rs, rowNum)-> {
                    int raterId = rs.getInt("rater_id");
                    int otherRaterId = rs.getInt("other_rater_id");
                    double similarityScore = rs.getDouble("similarity_score");
                    return new UserSimilarity(raterId,otherRaterId,similarityScore);
                })
                .fetchSize(500)
                .build();

    }

}
