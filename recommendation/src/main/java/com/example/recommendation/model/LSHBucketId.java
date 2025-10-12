package com.example.recommendation.model;

import java.io.Serializable;
import java.util.Objects;

public class LSHBucketId implements Serializable {
    private int raterId;
    private long bucketId;

    public LSHBucketId() {}

    public LSHBucketId(int raterId, long bucketId) {
        this.raterId = raterId;
        this.bucketId = bucketId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LSHBucketId)) return false;
        LSHBucketId that = (LSHBucketId) o;
        return raterId == that.raterId && bucketId == that.bucketId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(raterId, bucketId);
    }
}
