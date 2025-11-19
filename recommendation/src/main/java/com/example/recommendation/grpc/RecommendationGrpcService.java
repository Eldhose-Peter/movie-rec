package com.example.recommendation.grpc;

import com.example.recommendation.Recommendation;
import com.example.recommendation.RecommendationRequest;
import com.example.recommendation.RecommendationResponse;
import com.example.recommendation.RecommendationServiceGrpc;
import com.example.recommendation.model.UserRecommendation;
import com.example.recommendation.service.UserRecommendationService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;


@GrpcService
public class RecommendationGrpcService extends RecommendationServiceGrpc.RecommendationServiceImplBase {

    private final UserRecommendationService service;

    public RecommendationGrpcService(UserRecommendationService service) {
        this.service = service;
    }

    @Override
    public void getRecommendations(RecommendationRequest request, StreamObserver<RecommendationResponse> responseObserver) {
        long userId = request.getUserId();
        int limit = request.getLimit() > 0 ? request.getLimit() : 50;
        int offset = request.getOffset();

        List<UserRecommendation> rows = service.getRecommendations(userId,limit,offset);

        RecommendationResponse.Builder builder = RecommendationResponse.newBuilder();
        for (UserRecommendation row : rows) {
            Recommendation rec = Recommendation.newBuilder()
                    .setUserId(row.getUserId())
                    .setMovieId(row.getMovieId())
                    .setWeightedSumTotal(row.getWeightedSumTotal())
                    .setWeightTotal(row.getWeightTotal())
                    .build();
            builder.addRecommendations(rec);
        }
        builder.setCount(rows.size());

        responseObserver.onNext(builder.build());
        responseObserver.onCompleted();
    }

}
