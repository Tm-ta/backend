package com.example.tmta.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@ConfigurationProperties(prefix = "tmta.storage.s3")
public class S3StorageProperties {

    private String region = "ap-northeast-2";
    private String bucket;
    private long presignExpirationSeconds = 300;
    private List<String> allowedContentTypes = new ArrayList<>();

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public long getPresignExpirationSeconds() {
        return presignExpirationSeconds;
    }

    public void setPresignExpirationSeconds(long presignExpirationSeconds) {
        this.presignExpirationSeconds = presignExpirationSeconds;
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        if (allowedContentTypes == null) {
            this.allowedContentTypes = new ArrayList<>();
            return;
        }
        this.allowedContentTypes = allowedContentTypes.stream()
                .map(contentType -> contentType == null ? null : contentType.toLowerCase(Locale.ROOT))
                .filter(contentType -> contentType != null && !contentType.isBlank())
                .toList();
    }
}
