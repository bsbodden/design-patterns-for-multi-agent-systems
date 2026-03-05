package com.redis.demos.smarttriage;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRedisDocumentRepositories
public class SmartTriageApp {

    public static void main(String[] args) {
        SpringApplication.run(SmartTriageApp.class, args);
    }
}
