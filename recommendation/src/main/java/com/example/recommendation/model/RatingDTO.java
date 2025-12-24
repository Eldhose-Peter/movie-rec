package com.example.recommendation.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class RatingDTO {
    private Integer raterId;
    private Integer movieId;
    private Double rating;
    private LocalDateTime timestamp;
}