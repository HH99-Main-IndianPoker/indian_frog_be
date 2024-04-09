package com.service.indianfrog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class IndianFrogApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndianFrogApplication.class, args);
    }

}