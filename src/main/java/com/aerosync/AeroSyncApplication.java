package com.aerosync;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AeroSyncApplication{
    public static void main(String[] args) {
        SpringApplication.run(AeroSyncApplication.class, args);
    }

}