package com.example.recommendation.grpc;

import com.example.recommendation.GreeterGrpc;
import com.example.recommendation.HelloRequest;
import com.example.recommendation.HelloResponse;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class GreeterService extends GreeterGrpc.GreeterImplBase {

    @Override
    public void sayHello(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
        String name = request.getName();
        String message = "Hello, " + name + "!";
        HelloResponse response = HelloResponse.newBuilder().setMessage(message).build();
        // Send the response
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
