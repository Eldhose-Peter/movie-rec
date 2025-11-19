package com.example.movie_service.grpc;

import com.example.movie_service.GreeterGrpc;
import com.example.movie_service.HelloRequest;
import com.example.movie_service.HelloResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.stereotype.Service;

@Service
public class GreeterClient {

    private final GreeterGrpc.GreeterBlockingStub blockingStub;
    public GreeterClient() {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();
        this.blockingStub = GreeterGrpc.newBlockingStub(channel);
    }
    public String sayHello(String name) {
        HelloRequest request = HelloRequest.newBuilder().setName(name).build();
        HelloResponse response = blockingStub.sayHello(request);
        return response.getMessage();
    }
}
