package com.example.recommendation.controller;

import com.example.recommendation.model.SimilarItem;
import com.example.recommendation.service.RaterSimilarityBatchService;
import com.example.recommendation.service.UserSimilarityApproxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/similarity")
public class SimilarityController {
    private final UserSimilarityApproxService userSimilarityApproxService;

    public SimilarityController(UserSimilarityApproxService userSimilarityApproxService) {
        this.userSimilarityApproxService = userSimilarityApproxService;
    }

//    @GetMapping("/user/{id}")
//    public String getSimilarRaters(@PathVariable Integer id){
//        log.info("Recieved request to get similar raters for a user");
//        return this.userSimilarityApproxService.getTopNSimilarRaters(id);
//    }

    @GetMapping("/compute")
    public String getSimilarRaters(){
        log.info("Recieved request to compute similarity");
        this.userSimilarityApproxService.generateSimilarity();
        return "OK";
    }

}
