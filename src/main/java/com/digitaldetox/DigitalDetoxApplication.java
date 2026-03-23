package com.digitaldetox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DigitalDetoxApplication {
    public static void main(String[] args) {
        SpringApplication.run(DigitalDetoxApplication.class, args);
    }
}