package com.example.recommendation.repository;

import com.example.recommendation.model.ImdbRatingEvent;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public interface RatingRepository extends Repository<ImdbRatingEvent, Long> {
    @Query("SELECT DISTINCT r.id.movieId FROM ImdbRatingEvent r")
    List<Integer> getUniqueMovieIds();

    @Query("SELECT DISTINCT r.id.raterId FROM ImdbRatingEvent r")
    List<Integer> getUniqueRaterIds();

    List<ImdbRatingEvent> findAll();

    List<ImdbRatingEvent> findById_RaterId(Integer raterId);

    @Query("SELECT r FROM ImdbRatingEvent r WHERE r.id.raterId IN :raterIds")
    List<ImdbRatingEvent> findByRaterIds(@Param("raterIds") Set<Integer> raterIds);


    ImdbRatingEvent findById_RaterIdAndId_MovieId(Integer raterId, Integer movieId);

    @Query(value = "SELECT r FROM ImdbRatingEvent r ORDER BY r.id.raterId OFFSET :offset LIMIT :limit", nativeQuery = true)
    List<ImdbRatingEvent> findBatch(@Param("offset") int offset, @Param("limit") int limit);

    @Query("SELECT r FROM ImdbRatingEvent r WHERE r.id.raterId <> :currentRaterId ORDER BY r.id.raterId")
    Stream<ImdbRatingEvent> streamAllExcept(@Param("currentRaterId") Integer currentRaterId);

    @Query("SELECT COUNT(r) FROM ImdbRatingEvent r")
    Long getTotalRatings();

    @Query("SELECT COUNT(DISTINCT r.id.movieId) FROM ImdbRatingEvent r")
    Long getTotalMovies();

    @Query("SELECT COUNT(DISTINCT r.id.raterId) FROM ImdbRatingEvent r")
    Long getTotalUsers();

    @Query("SELECT r FROM ImdbRatingEvent r WHERE r.id.raterId = :raterId")
    List<ImdbRatingEvent> findByRaterId(Integer raterId);
}
