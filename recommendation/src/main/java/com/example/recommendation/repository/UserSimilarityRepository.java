package com.example.recommendation.repository;


import com.example.recommendation.model.UserSimilarity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface UserSimilarityRepository extends Repository<UserSimilarity, Long> {
    UserSimilarity save(UserSimilarity userSimilarity);

    @Modifying
    @Query("DELETE FROM UserSimilarity u WHERE u.id.raterId = ?1 ")
    void deleteByRaterId(int raterId);
}
