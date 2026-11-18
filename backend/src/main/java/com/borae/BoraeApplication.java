package com.borae;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BoraeApplication {

    public static void main(String[] args) {
        SpringApplication.run(BoraeApplication.class, args);
    }
}
