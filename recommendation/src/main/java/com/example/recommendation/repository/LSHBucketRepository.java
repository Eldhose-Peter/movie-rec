package com.example.recommendation.repository;

import com.example.recommendation.model.LSHBucket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface LSHBucketRepository extends JpaRepository<LSHBucket, Long> {
    void deleteByRaterId(int raterId);

}   
