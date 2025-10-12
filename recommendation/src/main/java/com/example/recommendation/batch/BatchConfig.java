package com.example.recommendation.batch;

import com.example.recommendation.batch.step3.GenerateCandidatePairsTasklet;
import com.example.recommendation.model.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.support.DefaultBatchConfiguration;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;


@Configuration
public class BatchConfig extends DefaultBatchConfiguration {
    // Step 1: compute signatures
    @Bean
    public Step computeSignaturesStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      JdbcCursorItemReader<UserMovies> userMoviesReader,
                                      ItemProcessor<UserMovies, UserSignature> userSignatureProcessor,
                                      JdbcBatchItemWriter<UserSignature> userSignatureWriter,
                                      ChunkLoggingListener chunkListener ) {

        return new StepBuilder("computeSignaturesStep", jobRepository)
                .<UserMovies, UserSignature>chunk(1000, transactionManager)
                .reader(userMoviesReader)
                .processor(userSignatureProcessor)
                .writer(userSignatureWriter)
                .listener(chunkListener)
                .build();
    }

    @Bean
    public Step generateBucketsStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    JdbcCursorItemReader<UserSignature> userSignatureReader,
                                    ItemProcessor<UserSignature, LSHBucket> lshProcessor,
                                    JdbcBatchItemWriter<LSHBucket> lshWriter,
                                    ChunkLoggingListener chunkListener ) {

        return new StepBuilder("generateBucketsStep", jobRepository)
                .<UserSignature, LSHBucket>chunk(2000, transactionManager)
                .reader(userSignatureReader)
                .processor(lshProcessor)
                .writer(lshWriter)
                .listener(chunkListener)
                .build();
    }

    @Bean
    public Step generateCandidatePairsStep(JobRepository jobRepository,
                                           PlatformTransactionManager transactionManager,
                                           GenerateCandidatePairsTasklet tasklet) {
        return new StepBuilder("generateCandidatePairsStep", jobRepository)
                .tasklet(tasklet, transactionManager)
                .build();
    }

    @Bean
    public Step computeSimilarityStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      JdbcCursorItemReader<UserSimilarityKey> candidateCursorReader,
                                      ItemProcessor<UserSimilarityKey, UserSimilarity> similarityInMemoryProcessor,
                                      JdbcBatchItemWriter<UserSimilarity> similarityWriter,
                                      ChunkLoggingListener chunkListener
    ) {
        return new StepBuilder("computeSimilarityStep", jobRepository)
                .<UserSimilarityKey, UserSimilarity>chunk(500, transactionManager)
                .reader(candidateCursorReader)
                .processor(similarityInMemoryProcessor)
                .writer(similarityWriter)
                .listener(chunkListener)
                .build();
    }

    @Bean
    public Job similarityJob(JobRepository jobRepository,
                             Step computeSignaturesStep,
                             Step generateBucketsStep,
                             Step generateCandidatePairsStep,
                             Step computeSimilarityStep) {
        return new JobBuilder("similarityJob", jobRepository)
                .start(computeSignaturesStep)
                .next(generateBucketsStep)
                .next(generateCandidatePairsStep)
                .next(computeSimilarityStep)
                .build();
    }

}

