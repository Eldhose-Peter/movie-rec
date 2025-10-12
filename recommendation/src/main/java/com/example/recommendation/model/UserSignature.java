package com.example.recommendation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Table(name = "user_signature")
@Getter
public class UserSignature {
    @Id
    int raterId;
    int[] signature;

    public UserSignature(int raterId, int[] signature){
        this.raterId = raterId;
        this.signature = signature;

    }
}
