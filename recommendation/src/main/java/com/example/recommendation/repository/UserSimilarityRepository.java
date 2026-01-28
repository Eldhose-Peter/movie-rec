package com.example.recommendation.repository;


import com.example.recommendation.model.UserSimilarity;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface UserSimilarityRepository extends Repository<UserSimilarity, Long> {
    UserSimilarity save(UserSimilarity userSimilarity);

    @Modifying
    @Query("DELETE FROM UserSimilarity u WHERE u.id.raterId = ?1 ")
    void deleteByRaterId(int raterId);

    @Query("SELECT u FROM UserSimilarity u WHERE u.id.raterId = ?1 ORDER BY u.similarityScore DESC LIMIT 50")
    List<UserSimilarity> findByIdRaterIdOrderBySimilarityScoreDesc(int raterId);
}
