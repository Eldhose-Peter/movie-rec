package com.example.recommendation.batch.step2;

import com.example.recommendation.model.UserSignature;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Arrays;

@Component
public class SignatureReader {
    @Bean
    @StepScope
    public JdbcCursorItemReader<UserSignature> userSignatureReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<UserSignature>()
                .name("userSignatureReader")
                .dataSource(dataSource)
                .sql("SELECT rater_id, signature FROM user_signature ORDER BY rater_id")
                .rowMapper((rs, rowNum) -> {
                    int userId = rs.getInt("rater_id");
                    Integer[] arr = (Integer[]) rs.getArray("signature").getArray();
                    int[] sig = Arrays.stream(arr).mapToInt(Integer::intValue).toArray();
                    return new UserSignature(userId, sig);
                })
                .fetchSize(2000)
                .build();
    }

}
