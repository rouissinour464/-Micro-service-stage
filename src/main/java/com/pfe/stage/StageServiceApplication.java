package com.pfe.stage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = "com.pfe.stage.client")
@SpringBootApplication
public class StageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(StageServiceApplication.class, args);
    }
}