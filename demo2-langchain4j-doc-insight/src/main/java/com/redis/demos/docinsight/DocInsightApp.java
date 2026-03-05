package com.redis.demos.docinsight;

import com.redis.om.spring.annotations.EnableRedisDocumentRepositories;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableRedisDocumentRepositories(basePackages = "com.redis.demos.docinsight.*")
public class DocInsightApp {

    public static void main(String[] args) {
        SpringApplication.run(DocInsightApp.class, args);
    }
}
