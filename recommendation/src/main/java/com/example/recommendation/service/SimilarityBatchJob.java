package com.example.recommendation.service;

/*
CREATE TABLE similarity (
    rater_id      INT NOT NULL,
    other_rater_id INT NOT NULL,
    similarity    DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (rater_id, other_rater_id)
);

CREATE TABLE job_checkpoint (
    job_name VARCHAR(100) PRIMARY KEY,
    last_rater_id INT
);


 */

/*
@Service
public class SimilarityBatchJob {

    private final RatingRepository ratingRepository;
    private final SimilarityRepository similarityRepository;
    private final CheckpointRepository checkpointRepository;

    private static final int TOP_N = 50;

    public void runBatchJob() {
        int lastProcessed = checkpointRepository.getLastProcessed("similarity-job").orElse(0);

        // Outer loop: iterate all raters
        List<Integer> allRaterIds = ratingRepository.getAllRaterIdsGreaterThan(lastProcessed);

        for (Integer raterId : allRaterIds) {
            log.info("Computing similarity for rater {}", raterId);

            try {
                computeAndStoreSimilarities(raterId);
                checkpointRepository.save("similarity-job", raterId);
            } catch (Exception e) {
                log.error("Failed at rater {}", raterId, e);
                break; // stop job, can resume later
            }
        }
    }

    private void computeAndStoreSimilarities(Integer currentRaterId) {
        Map<Integer, Double> currentRatings = ratingRepository.findById_RaterId(currentRaterId).stream()
                .collect(Collectors.toMap(RatingEvent::getMovieId, r -> r.getRating() - 5));

        if (currentRatings.isEmpty()) {
            return;
        }

        PriorityQueue<SimilarItem> topSimilar = new PriorityQueue<>(Comparator.comparing(SimilarItem::getSimilarity));

        try (Stream<RatingEvent> stream = ratingRepository.streamAllExceptOrdered(currentRaterId)) {
            List<RatingEvent> buffer = new ArrayList<>();
            Integer lastRater = null;

            for (Iterator<RatingEvent> it = stream.iterator(); it.hasNext();) {
                RatingEvent rating = it.next();
                if (lastRater == null) {
                    lastRater = rating.getRaterId();
                }
                if (!rating.getRaterId().equals(lastRater)) {
                    processSingleRater(lastRater, buffer, currentRatings, topSimilar);
                    buffer.clear();
                    lastRater = rating.getRaterId();
                }
                buffer.add(rating);
            }
            if (!buffer.isEmpty()) {
                processSingleRater(lastRater, buffer, currentRatings, topSimilar);
            }
        }

        // persist top-N results
        similarityRepository.deleteByRaterId(currentRaterId);
        similarityRepository.saveAll(
                topSimilar.stream()
                        .map(item -> new SimilarityEntity(currentRaterId, item.getOtherRaterId(), item.getSimilarity()))
                        .collect(Collectors.toList())
        );
    }

    private void processSingleRater(Integer otherRaterId,
                                    List<RatingEvent> otherRatings,
                                    Map<Integer, Double> currentRatings,
                                    PriorityQueue<SimilarItem> topSimilar) {
        double dot = 0.0;
        for (RatingEvent rating : otherRatings) {
            Double cur = currentRatings.get(rating.getMovieId());
            if (cur != null) {
                dot += cur * (rating.getRating() - 5);
            }
        }
        if (dot > 0) {
            if (topSimilar.size() < TOP_N) {
                topSimilar.add(new SimilarItem(otherRaterId, dot));
            } else if (dot > topSimilar.peek().getSimilarity()) {
                topSimilar.poll();
                topSimilar.add(new SimilarItem(otherRaterId, dot));
            }
        }
    }
}
*/