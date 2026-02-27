package com.example.tmta.auth.social;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "tmta.auth.social")
public class SocialAuthProperties {

    private Provider google = new Provider();
    private Provider apple = new Provider();
    private UserInfoProvider kakao = new UserInfoProvider();
    private UserInfoProvider naver = new UserInfoProvider();

    public Provider getGoogle() {
        return google;
    }

    public void setGoogle(Provider google) {
        this.google = google;
    }

    public Provider getApple() {
        return apple;
    }

    public void setApple(Provider apple) {
        this.apple = apple;
    }

    public UserInfoProvider getKakao() {
        return kakao;
    }

    public void setKakao(UserInfoProvider kakao) {
        this.kakao = kakao;
    }

    public UserInfoProvider getNaver() {
        return naver;
    }

    public void setNaver(UserInfoProvider naver) {
        this.naver = naver;
    }

    public static class Provider {
        private boolean enabled;
        private String issuer;
        private String jwksUri;
        private List<String> audiences = new ArrayList<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getIssuer() {
            return issuer;
        }

        public void setIssuer(String issuer) {
            this.issuer = issuer;
        }

        public String getJwksUri() {
            return jwksUri;
        }

        public void setJwksUri(String jwksUri) {
            this.jwksUri = jwksUri;
        }

        public List<String> getAudiences() {
            return audiences;
        }

        public void setAudiences(List<String> audiences) {
            this.audiences = audiences == null ? new ArrayList<>() : audiences;
        }
    }

    public static class UserInfoProvider {
        private boolean enabled;
        private String userInfoUri;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getUserInfoUri() {
            return userInfoUri;
        }

        public void setUserInfoUri(String userInfoUri) {
            this.userInfoUri = userInfoUri;
        }
    }
}
