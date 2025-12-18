package com.example.recommendation.consumer;

import com.example.recommendation.config.RabbitConfig;
import com.example.recommendation.model.RatingEvent;
import com.example.recommendation.repository.RatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RatingEventListener {

    private final RatingRepository ratingRepository;

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void handleRatingEvent(RatingEvent event) {
        // Logic to save to ratings db
        System.out.println("Processing rating from User: " + event.getRaterId());

        // ... save logic ...
    }
}
