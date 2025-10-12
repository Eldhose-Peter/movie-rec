package com.example.recommendation.controller;

import com.example.recommendation.model.DatabaseDetails;
import com.example.recommendation.service.RatingTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ratings")
public class RatingController {
    private final RatingTestService ratingTestService;

    public RatingController(RatingTestService ratingTestService) {
        this.ratingTestService = ratingTestService;
    }

    @GetMapping("/all")
    public List<Integer> getSimilarRaters() {
        log.info("Recieved request to get all rater IDs");
        return this.ratingTestService.getAllRaterIds();
    }

    @GetMapping("/details")
    public DatabaseDetails getDatabaseDetails(){
        return this.ratingTestService.getDetails();
    }
}

