package com.example.movie_service.grpc;


import com.example.movie_service.Recommendation;
import com.example.movie_service.RecommendationRequest;
import com.example.movie_service.RecommendationResponse;
import com.example.movie_service.RecommendationServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecommendationGrpcClient {

    // Inject generated blocking stub (name must match config key)
    @GrpcClient("recommendation")
    private RecommendationServiceGrpc.RecommendationServiceBlockingStub blockingStub;

    public List<Recommendation> getRecommendations(long userId, int limit, int offset) {
        RecommendationRequest req = RecommendationRequest.newBuilder()
                .setUserId(userId)
                .setLimit(limit)
                .setOffset(offset)
                .build();

        RecommendationResponse resp = blockingStub.getRecommendations(req);

        return resp.getRecommendationsList();
    }
}

