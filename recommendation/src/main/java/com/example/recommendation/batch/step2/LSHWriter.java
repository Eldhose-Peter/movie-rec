package com.example.recommendation.batch.step2;

import com.example.recommendation.model.LSHBucket;
import com.example.recommendation.service.lsh.LSHService;
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

@Component
public class LSHWriter {

    @Bean
    public JdbcBatchItemWriter<LSHBucket> delegateBucketWriter(DataSource dataSource) {
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

    @Bean
    @StepScope
    public ItemWriter<List<LSHBucket>> lshBucketWriter(JdbcBatchItemWriter<LSHBucket> delegate) {
        return chunk -> {

            List<LSHBucket> flattenedItems = new ArrayList<>();
            for (List<LSHBucket> list : chunk) {
                flattenedItems.addAll(list);
            }

            // In Spring Batch 5+, we must wrap the list back into a Chunk
            // If you are on Spring Batch 4, you can pass the list directly
            delegate.write(new Chunk<>(flattenedItems));
        };
    }

}
