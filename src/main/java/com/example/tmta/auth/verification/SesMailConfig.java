package com.example.tmta.auth.verification;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;

@Configuration
@Profile("prod")
@ConditionalOnProperty(prefix = "tmta.email.ses", name = "enabled", havingValue = "true")
public class SesMailConfig {

    @Bean
    public SesV2Client sesV2Client(SesMailProperties properties) {
        return SesV2Client.builder()
                .region(Region.of(properties.getRegion()))
                .build();
    }
}
