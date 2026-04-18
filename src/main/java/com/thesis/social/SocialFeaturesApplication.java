package com.thesis.social;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class SocialFeaturesApplication {

    public static void main(String[] args) {
        SpringApplication.run(SocialFeaturesApplication.class, args);
    }
}
