package com.example.movie_service.controller;

import com.example.movie_service.grpc.GreeterClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GreeterController {
    private final GreeterClient greeterClient;

    @Autowired
    public GreeterController(GreeterClient greeterClient) {
        this.greeterClient = greeterClient;
    }

    @GetMapping("/greet")
    public String greet(@RequestParam String name) {
        return greeterClient.sayHello(name);
    }
}
