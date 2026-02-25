package com.example.tmta.auth.social;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SocialHttpClientConfig {

    @Bean
    public RestClient socialRestClient(RestClient.Builder builder) {
        return builder.build();
    }
}
