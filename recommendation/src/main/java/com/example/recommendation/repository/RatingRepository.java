package com.example.recommendation.repository;

import com.example.recommendation.model.RatingEvent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.stream.Stream;
import java.util.Optional;

public interface RatingRepository extends Repository<RatingEvent, Long> {
    @Query("SELECT DISTINCT r.id.movieId FROM RatingEvent r")
    List<Integer> getUniqueMovieIds();

    @Query("SELECT DISTINCT r.id.raterId FROM RatingEvent r")
    List<Integer> getUniqueRaterIds();

    List<RatingEvent> findAll();

    List<RatingEvent> findById_RaterId(Integer raterId);

    RatingEvent findById_RaterIdAndId_MovieId(Integer raterId, Integer movieId);

    @Query(value = "SELECT r FROM RatingEvent r ORDER BY r.id.raterId OFFSET :offset LIMIT :limit", nativeQuery = true)
    List<RatingEvent> findBatch(@Param("offset") int offset, @Param("limit") int limit);

    @Query("SELECT r FROM RatingEvent r WHERE r.id.raterId <> :currentRaterId ORDER BY r.id.raterId")
    Stream<RatingEvent> streamAllExcept(@Param("currentRaterId") Integer currentRaterId);

    @Query("SELECT COUNT(r) FROM RatingEvent r")
    Long getTotalRatings();

    @Query("SELECT COUNT(DISTINCT r.id.movieId) FROM RatingEvent r")
    Long getTotalMovies();

    @Query("SELECT COUNT(DISTINCT r.id.raterId) FROM RatingEvent r")
    Long getTotalUsers();
}
