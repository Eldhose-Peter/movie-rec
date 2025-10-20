package com.example.recommendation.batch.step5;

import com.example.recommendation.model.MovieWeightContribution;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MovieSimilarityWriter {

    @Bean
    @StepScope
    public ItemWriter<List<MovieWeightContribution>> flatteningWriter(JdbcBatchItemWriter<MovieWeightContribution> movieSimilarityJdbcWriter) {
        return chunk -> {
            List<MovieWeightContribution> flatList = new ArrayList<>();

            log.info("Chunk size recieved is {},", chunk.size());

            for (List<MovieWeightContribution> list : chunk) {
                flatList.addAll(list);  // flatten nested lists
            }

            log.info("Processed similarity for user , movies = {}" ,flatList.size());

            if (!flatList.isEmpty()) {
                Chunk<MovieWeightContribution> flatChunk = new Chunk<>(flatList);
                movieSimilarityJdbcWriter.write(flatChunk);
            }
        };
    }


    @Bean
    @StepScope
    public JdbcBatchItemWriter<MovieWeightContribution> movieSimilarityJdbcWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<MovieWeightContribution>()
                .itemSqlParameterSourceProvider(new org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("""
                    INSERT INTO user_recommendations (user_id, movie_id, weighted_sum_total, weight_total)
                    VALUES (:userId, :movieId, :weightedSum, :weightSum)
                    ON CONFLICT (user_id, movie_id)
                    DO UPDATE SET
                        weighted_sum_total = user_recommendations.weighted_sum_total + EXCLUDED.weighted_sum_total,
                        weight_total = user_recommendations.weight_total + EXCLUDED.weight_total;
                    """)
                .dataSource(dataSource)
                .build();
    }

}
