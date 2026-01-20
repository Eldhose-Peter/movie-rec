package com.example.recommendation.batch.step2;

import com.example.recommendation.model.LSHBucket;
import com.example.recommendation.model.UserSignature;
import com.example.recommendation.service.lsh.LSHService;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hibernate.internal.util.collections.ArrayHelper.forEach;

@Component
public class LSHProcessor {

    @Bean
    @StepScope
    public ItemProcessor<UserSignature, List<LSHBucket>> lshBucketProcessor(LSHService lshService) {
        return userSig -> {
            List<LSHService.BucketEntry> entries = lshService.computeBuckets(userSig.getRaterId(), userSig.getSignature());
            List<LSHBucket> buckets = new ArrayList<>();
            entries.stream().map(bucketEntry ->
                    new LSHBucket(bucketEntry.bucketId(), bucketEntry.userId())
            ).forEach(buckets::add);

            return buckets;
        };
    }

}
