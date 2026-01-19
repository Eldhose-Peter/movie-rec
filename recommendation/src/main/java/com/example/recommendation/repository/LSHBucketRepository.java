package com.example.recommendation.repository;

import com.example.recommendation.model.LSHBucket;
import org.springframework.data.repository.Repository;

public interface LSHBucketRepository extends Repository<LSHBucket, Long> {
    LSHBucket save(LSHBucket lshBucket);
    void deleteByRaterId(int raterId);

}
