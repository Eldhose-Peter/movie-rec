package com.example.recommendation.batch.step5;

import com.example.recommendation.model.MovieWeightContribution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;


public class MovieSimilarityWriter {

    @Bean
    @StepScope
    public ItemWriter<List<MovieWeightContribution>> flatteningWriter(JdbcBatchItemWriter<MovieWeightContribution> movieSimilarityWriter) {
        return chunk -> {
            List<MovieWeightContribution> flatList = new ArrayList<>();

            for (List<MovieWeightContribution> list : chunk) {
                flatList.addAll(list);  // flatten nested lists
            }

            if (!flatList.isEmpty()) {
                Chunk<MovieWeightContribution> flatChunk = new Chunk<>(flatList);
                movieSimilarityWriter.write(flatChunk);
            }
        };
    }


    @Bean
    @StepScope
    public JdbcBatchItemWriter<MovieWeightContribution> movieSimilarityWriter(DataSource dataSource) {
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
