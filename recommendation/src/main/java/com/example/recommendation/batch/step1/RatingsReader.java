package com.example.recommendation.batch.step1;

import com.example.recommendation.model.UserMovies;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Configuration
public class RatingsReader {
    @Bean
    @StepScope
    public JdbcCursorItemReader<UserMovies> userRatingsReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<UserMovies>()
                .name("userRatingsReader")
                .dataSource(dataSource)
                .sql("""
            SELECT rater_id, array_agg(movie_id ORDER BY movie_id) AS movies
            FROM ratings
            GROUP BY rater_id
            ORDER BY rater_id
        """)
                .rowMapper((rs, rowNum) -> {
                    int userId = rs.getInt("rater_id");
                    Integer[] moviesArr = (Integer[]) rs.getArray("movies").getArray();
                    Set<Integer> movies = new HashSet<>(Arrays.asList(moviesArr));
                    return new UserMovies(userId, movies);
                })
                .fetchSize(1000)
                .build();
    }

}
