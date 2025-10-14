package com.example.recommendation.batch.step2;

import com.example.recommendation.model.LSHBucket;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class LSHWriter {

    @Bean
    @StepScope
    public JdbcBatchItemWriter<LSHBucket> lshBucketWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<LSHBucket>()
                .dataSource(dataSource)
                .sql("""
            INSERT INTO lsh_bucket (bucket_id, rater_id)
            VALUES (:bucketId, :raterId)
            ON CONFLICT DO NOTHING
        """)
                .beanMapped()
                .assertUpdates(false) // allow zero updates on rows
                .build();
    }

}
