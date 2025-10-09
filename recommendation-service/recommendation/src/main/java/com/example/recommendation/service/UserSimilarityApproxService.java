package com.example.recommendation.service;

import com.example.recommendation.model.RatingEvent;
import com.example.recommendation.model.UserSignature;
import com.example.recommendation.repository.RatingRepository;
import com.example.recommendation.service.lsh.CandidateGenerator;
import com.example.recommendation.service.lsh.LSHService;
import com.example.recommendation.service.lsh.MinHasher;
import com.example.recommendation.service.lsh.Similarity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserSimilarityApproxService {
    int numHashes = 128;
    int bands = 32, rowsPerBand = 4;
    RatingRepository ratingRepository;
    MinHasher minHasher;

    public UserSimilarityApproxService(RatingRepository ratingRepository){
        this.ratingRepository = ratingRepository;
        this.minHasher = new MinHasher(numHashes);
    }

    public void generateSimilarity(){

        log.info("Collectng rating infor from DB");
        List<RatingEvent> allRatings = ratingRepository.findAll();

        log.info("Creating a mapping of raters");

        Map<Integer, List<RatingEvent>> ratingsByRater = allRatings.stream()
                .collect(Collectors.groupingBy(RatingEvent::getRaterId));

        log.info("Building signatures");
        // Step 1: Build signatures
        List<UserSignature> signatures= ratingsByRater.entrySet().stream().map(entry -> {
            int userId = entry.getKey();
            Set<Integer> movies = entry.getValue().stream().map(RatingEvent::getMovieId).collect(Collectors.toSet());
            int[] sig = minHasher.computeSignature(movies);
            return new UserSignature(userId, sig);
        }).toList();

        log.info("LSH Bucketing");
        // Step 2: LSH Bucketing
        LSHService lshService = new LSHService(bands, rowsPerBand);
        Map<String, List<Integer>> buckets = lshService.bucketUsers(signatures);

        log.info("Generating candidate pairs");
        //Step 3: Candidate pairs
        CandidateGenerator generator = new CandidateGenerator();
        Set<String> candidates = generator.generateCandidates(buckets);

        log.info("Computing similarities for candidates");
        //Step 4: Compute similarities only for candidates
        for (String pair : candidates) {
            String[] ids = pair.split("-");
            int u1 = Integer.parseInt(ids[0]);
            int u2 = Integer.parseInt(ids[1]);

            List<RatingEvent> userRatings1 = ratingsByRater.get(u1);
            List<RatingEvent> userRatings2 = ratingsByRater.get(u2);

            Map<Integer, Double> userRatingMap1 = userRatings1.stream().collect(Collectors.toMap(RatingEvent::getMovieId, RatingEvent::getRating));
            Map<Integer, Double> userRatingMap2 = userRatings2.stream().collect(Collectors.toMap(RatingEvent::getMovieId, RatingEvent::getRating));


            double sim = Similarity.cosine(userRatingMap1, userRatingMap2);
            // TODO: Store top-K in DB or heap

            log.info("Similarity computed " + pair + " : " + sim);
        }

    }









}
