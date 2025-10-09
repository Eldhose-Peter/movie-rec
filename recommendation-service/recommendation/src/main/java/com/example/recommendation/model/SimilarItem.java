package com.example.recommendation.model;

import lombok.Getter;

public class SimilarItem implements Comparable<SimilarItem>{
    @Getter
    private final Integer itemId;
    @Getter
    private final Double similarity;

    public SimilarItem(Integer itemId, Double similarity){
        this.itemId = itemId;
        this.similarity = similarity;
    }

    @Override
    public int compareTo(SimilarItem other){
        return Double.compare(this.similarity, other.similarity);
    }
}
