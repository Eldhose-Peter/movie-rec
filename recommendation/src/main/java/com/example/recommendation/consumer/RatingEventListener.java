package com.example.recommendation.consumer;

import com.example.recommendation.config.RabbitConfig;
import com.example.recommendation.model.InternalRatingEvent;
import com.example.recommendation.repository.InternalRatingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RatingEventListener {

    private final InternalRatingRepository ratingRepository;

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void handleRatingEvent(InternalRatingEvent event) {
        // Logic to save to ratings db
        System.out.println("Processing rating from User: " + event.getRaterId());
        ratingRepository.save(event);
    }
}
