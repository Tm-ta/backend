package com.example.tmta.auth.social;

import com.example.tmta.auth.dto.SocialLoginRequest;
import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import com.example.tmta.member.entity.type.AuthProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class NaverSocialIdentityVerifier implements SocialIdentityVerifier {

    private final SocialAuthProperties properties;
    private final RestClient socialRestClient;
    private final ObjectMapper objectMapper;

    public NaverSocialIdentityVerifier(SocialAuthProperties properties, RestClient socialRestClient, ObjectMapper objectMapper) {
        this.properties = properties;
        this.socialRestClient = socialRestClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AuthProvider provider() {
        return AuthProvider.NAVER;
    }

    @Override
    public SocialIdentity verify(SocialLoginRequest request) {
        if (!properties.getNaver().isEnabled()) {
            throw new BusinessException(ErrorCode.SOCIAL_PROVIDER_NOT_SUPPORTED);
        }
        JsonNode body = callUserInfo(properties.getNaver().getUserInfoUri(), request.providerToken());
        JsonNode response = body.path("response");
        String id = response.path("id").asText(null);
        String email = response.path("email").asText(null);
        if (id == null || id.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_TOKEN_INVALID);
        }
        return new SocialIdentity(AuthProvider.NAVER, id, email);
    }

    private JsonNode callUserInfo(String uri, String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_TOKEN_INVALID);
        }
        try {
            String response = socialRestClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(response == null ? "{}" : response);
        } catch (RestClientException | java.io.IOException e) {
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_TOKEN_INVALID);
        }
    }
}
