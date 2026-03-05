package com.redis.demos.researchplanner;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRedisDocumentRepositories(basePackages = "com.redis.demos.researchplanner.*")
public class ResearchPlannerApp {

    public static void main(String[] args) {
        SpringApplication.run(ResearchPlannerApp.class, args);
    }
}
