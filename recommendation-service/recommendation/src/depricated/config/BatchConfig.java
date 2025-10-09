package References.similarity.config;

import com.example.similarity.model.*;
import References.similarity.service.LSHService;
import References.similarity.service.MinHasher;
import jakarta.sql.DataSource;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.database.*;
import org.springframework.batch.item.database.builder.*;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.support.BeanPropertyRowMapper;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;

@Configuration
public class BatchConfig {

    @Bean
    public JobRepository jobRepository(DataSource dataSource, PlatformTransactionManager transactionManager)
            throws Exception {
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(transactionManager);
        factory.afterPropertiesSet();
        return factory.getObject();
    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) {
        SimpleJobLauncher launcher = new SimpleJobLauncher();
        launcher.setJobRepository(jobRepository);
        return launcher;
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<UserRatings> userRatingsReader(DataSource dataSource) {
        String sql = "SELECT rater_id, array_agg(movie_id ORDER BY movie_id) AS movies FROM rating GROUP BY rater_id ORDER BY rater_id";
        JdbcCursorItemReader<UserRatings> reader = new JdbcCursorItemReaderBuilder<UserRatings>()
                .name("userRatingsReader")
                .dataSource(dataSource)
                .sql(sql)
                .rowMapper((rs, rowNum) -> {
                    int userId = rs.getInt("rater_id");
                    java.sql.Array arr = rs.getArray("movies");
                    Integer[] moviesArr = (Integer[]) arr.getArray();
                    Set<Integer> movies = new HashSet<>(Arrays.asList(moviesArr));
                    return new UserRatings(userId, movies);
                })
                .fetchSize(1000)
                .build();
        return reader;
    }

    @Bean
    @StepScope
    public ItemProcessor<UserRatings, UserSignatureEntity> signatureProcessor(MinHasher minHasher) {
        return user -> new UserSignatureEntity(user.userId(), minHasher.computeSignature(user.movies()));
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<UserSignatureEntity> userSignatureWriter(DataSource dataSource) {
        JdbcBatchItemWriter<UserSignatureEntity> writer = new JdbcBatchItemWriterBuilder<UserSignatureEntity>()
                .dataSource(dataSource)
                .sql("INSERT INTO user_signature (rater_id, signature) VALUES (:raterId, :signature) ON CONFLICT (rater_id) DO UPDATE SET signature = EXCLUDED.signature")
                .beanMapped()
                .build();
        return writer;
    }

    @Bean
    @StepScope
    public JdbcCursorItemReader<UserSignatureEntity> userSignatureReader(DataSource dataSource) {
        JdbcCursorItemReader<UserSignatureEntity> reader = new JdbcCursorItemReaderBuilder<UserSignatureEntity>()
                .name("userSignatureReader")
                .dataSource(dataSource)
                .sql("SELECT rater_id, signature FROM user_signature ORDER BY rater_id")
                .rowMapper(new BeanPropertyRowMapper<>(UserSignatureEntity.class))
                .fetchSize(2000)
                .build();
        return reader;
    }

    @Bean
    @StepScope
    public ItemProcessor<UserSignatureEntity, LshBucketEntity> lshBucketProcessor(LSHService lsh) {
        return userSig -> {
            Integer[] arr = userSig.getSignature();
            int[] sig = Arrays.stream(arr).mapToInt(Integer::intValue).toArray();
            long bucketId = lsh.computeBuckets(userSig.getRaterId(), sig).get(0).bucketId;
            return new LshBucketEntity(bucketId, userSig.getRaterId());
        };
    }

    @Bean
    @StepScope
    public JdbcBatchItemWriter<LshBucketEntity> lshBucketWriter(DataSource dataSource) {
        JdbcBatchItemWriter<LshBucketEntity> writer = new JdbcBatchItemWriterBuilder<LshBucketEntity>()
                .dataSource(dataSource)
                .sql("INSERT INTO lsh_bucket (bucket_id, rater_id) VALUES (:bucketId, :raterId) ON CONFLICT DO NOTHING")
                .beanMapped()
                .build();
        return writer;
    }

    @Bean
    public JdbcPagingItemReader<Map<String,Object>> candidateReader(DataSource dataSource) {
        JdbcPagingItemReader<Map<String,Object>> reader = new JdbcPagingItemReaderBuilder<Map<String,Object>>()
                .name("candidateReader")
                .dataSource(dataSource)
                .selectClause("SELECT rater_id, other_rater_id")
                .fromClause("FROM similarity_candidate")
                .sortKeys(Map.of("rater_id", org.springframework.batch.item.database.Order.ASCENDING))
                .pageSize(5000)
                .rowMapper((rs, rowNum) -> {
                    Map<String,Object> m = new HashMap<>();
                    m.put("rater_id", rs.getInt("rater_id"));
                    m.put("other_rater_id", rs.getInt("other_rater_id"));
                    return m;
                })
                .build();
        return reader;
    }

    @Bean
    public Step computeSignaturesStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      JdbcCursorItemReader<UserRatings> userRatingsReader,
                                      ItemProcessor<UserRatings, UserSignatureEntity> signatureProcessor,
                                      JdbcBatchItemWriter<UserSignatureEntity> userSignatureWriter) {

        return new StepBuilder("computeSignaturesStep", jobRepository)
                .<UserRatings, UserSignatureEntity>chunk(1000, transactionManager)
                .reader(userRatingsReader)
                .processor(signatureProcessor)
                .writer(userSignatureWriter)
                .build();
    }

    @Bean
    public Step generateBucketsStep(JobRepository jobRepository,
                                    PlatformTransactionManager transactionManager,
                                    JdbcCursorItemReader<UserSignatureEntity> userSignatureReader,
                                    ItemProcessor<UserSignatureEntity, LshBucketEntity> lshProcessor,
                                    JdbcBatchItemWriter<LshBucketEntity> lshWriter) {

        return new StepBuilder("generateBucketsStep", jobRepository)
                .<UserSignatureEntity, LshBucketEntity>chunk(2000, transactionManager)
                .reader(userSignatureReader)
                .processor(lshProcessor)
                .writer(lshWriter)
                .build();
    }

    @Bean
    public Step computeSimilarityStep(JobRepository jobRepository,
                                      PlatformTransactionManager transactionManager,
                                      JdbcPagingItemReader<Map<String,Object>> candidateReader) {
        return new StepBuilder("computeSimilarityStep", jobRepository)
                .<Map<String,Object>, Map<String,Object>>chunk(2000, transactionManager)
                .reader(candidateReader)
                .processor(item -> item)  // placeholder
                .writer(items -> {})      // placeholder
                .build();
    }

    @Bean
    public Job similarityJob(JobRepository jobRepository,
                             Step computeSignaturesStep,
                             Step generateBucketsStep,
                             Step computeSimilarityStep) {
        return new JobBuilder("similarityJob", jobRepository)
                .start(computeSignaturesStep)
                .next(generateBucketsStep)
                .next(computeSimilarityStep)
                .build();
    }
}
