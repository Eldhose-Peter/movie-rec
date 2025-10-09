package com.example.recommendation.batch.step1;

import com.example.recommendation.model.UserSignature;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class SignatureWriter {
    @Bean
    @StepScope
    public JdbcBatchItemWriter<UserSignature> userSignatureWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<UserSignature>()
                .dataSource(dataSource)
                .sql("""
            INSERT INTO user_signature (rater_id, signature)
            VALUES (:raterId, :signature)
            ON CONFLICT (rater_id) DO UPDATE SET signature = EXCLUDED.signature
        """)
                .beanMapped()
                .build();
    }

}
