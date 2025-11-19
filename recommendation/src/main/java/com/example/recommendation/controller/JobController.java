package com.example.recommendation.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobLauncher jobLauncher;
    private final Job similarityJob;

    @PostMapping("/start")
    public String runSimilarityJob(@RequestParam(required = false) String param) {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("run.id", System.currentTimeMillis()) // ensures a unique run
                    .addString("customParam", param)
                    .toJobParameters();

            jobLauncher.run(similarityJob, jobParameters);
            return "✅ Similarity job triggered successfully!";
        } catch (Exception e) {
            return "❌ Failed to start job: " + e.getMessage();
        }
    }
}

