package com.example.tmta;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class TmtaApplication {

    public static void main(String[] args) {
        SpringApplication.run(TmtaApplication.class, args);
    }

}
