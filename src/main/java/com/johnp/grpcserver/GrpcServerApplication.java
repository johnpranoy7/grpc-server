package com.johnp.grpcserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GrpcServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrpcServerApplication.class, args);
    }

}
