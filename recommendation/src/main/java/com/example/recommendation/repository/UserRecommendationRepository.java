package com.example.recommendation.repository;

import com.example.recommendation.model.UserRecommendation;
import com.example.recommendation.model.UserRecommendationKey;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRecommendationRepository
        extends JpaRepository<UserRecommendation, UserRecommendationKey> {

    @Query("select r from UserRecommendation r where r.id.userId = :userId order by r.weightedSumTotal desc")
    List<UserRecommendation> findTopByUser(@Param("userId") long userId);

    void deleteByIdUserId(long userId);


}
