package com.example.recommendation.batch.step4;

import com.example.recommendation.model.UserSimilarityKey;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

@Component
public class SimilarityReader {
    @Bean
    @StepScope
    public JdbcPagingItemReader<UserSimilarityKey> candidateReader(DataSource dataSource) {
        return new JdbcPagingItemReaderBuilder<UserSimilarityKey>()
                .name("candidateReader")
                .dataSource(dataSource)
                .selectClause("SELECT rater_id, other_rater_id")
                .fromClause("FROM similarity_candidate")
                .sortKeys(Map.of("rater_id", Order.ASCENDING))
                .rowMapper((rs, rowNum) ->
                        new UserSimilarityKey(rs.getInt("rater_id"), rs.getInt("other_rater_id")))
                .pageSize(5000)
                .build();
    }

}
