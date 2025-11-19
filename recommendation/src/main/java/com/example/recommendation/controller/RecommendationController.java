package com.example.recommendation.controller;

import com.example.recommendation.model.UserRecommendation;
import com.example.recommendation.service.UserRecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/recommendation")
public class RecommendationController {
    private final UserRecommendationService userRecommendationService;

    public RecommendationController(UserRecommendationService userRecommendationService) {
        this.userRecommendationService = userRecommendationService;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserRecommendation>> getUserRecommendations(@PathVariable Integer userId){
        log.info("Recieved request to get recommended movies for a user {}", userId);
        List<UserRecommendation> userRecommendationList = this.userRecommendationService.getRecommendations(userId,1,20);
        return ResponseEntity.ok(userRecommendationList);
    }

}
