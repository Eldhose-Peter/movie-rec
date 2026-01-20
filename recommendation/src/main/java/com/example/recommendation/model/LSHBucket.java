package com.example.recommendation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "lsh_bucket")
@Getter
@IdClass(LSHBucketId.class)
@NoArgsConstructor
public class LSHBucket {
    @Id
    private int raterId;

    @Id
    private long bucketId;

    public LSHBucket(long bucketId, int raterId) {
        this.raterId = raterId;
        this.bucketId = bucketId;
    }
}
