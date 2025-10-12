package com.example.recommendation.batch.step2;

import com.example.recommendation.model.LSHBucket;
import com.example.recommendation.model.UserSignature;
import com.example.recommendation.service.lsh.LSHService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LSHProcessor {

    @Bean
    @StepScope
    public ItemProcessor<UserSignature, LSHBucket> lshBucketProcessor(LSHService lshService) {
        return userSig -> {
            List<LSHService.BucketEntry> entries = lshService.computeBuckets(userSig.getRaterId(), userSig.getSignature());
            // return multiple LshBucketEntity objects for each band
            // you can wrap into a FlatListItemWriter later
            return new LSHBucket(entries.getFirst().bucketId(), entries.getFirst().userId());
        };
    }

}
