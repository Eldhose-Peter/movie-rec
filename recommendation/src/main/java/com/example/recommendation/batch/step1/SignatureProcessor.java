package com.example.recommendation.batch.step1;

import com.example.recommendation.model.UserMovies;
import com.example.recommendation.model.UserSignature;
import com.example.recommendation.service.lsh.MinHasher;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SignatureProcessor {
    @Bean
    @StepScope
    public ItemProcessor<UserMovies, UserSignature> userSignatureProcessor(MinHasher minHasher) {
        return user -> {
            int[] signature = minHasher.computeSignature(user.movies());
            return new UserSignature(user.userId(), signature);
        };
    }

}
