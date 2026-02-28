package com.example.tmta.storage;

import com.example.tmta.common.exception.BusinessException;
import com.example.tmta.common.exception.ErrorCode;
import com.example.tmta.storage.dto.PresignUploadRequest;
import com.example.tmta.storage.dto.PresignUploadResponse;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
public class S3PresignService {

    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final S3Presigner s3Presigner;
    private final S3StorageProperties properties;

    public S3PresignService(S3Presigner s3Presigner, S3StorageProperties properties) {
        this.s3Presigner = s3Presigner;
        this.properties = properties;
    }

    public PresignUploadResponse createUploadUrl(Long memberId, PresignUploadRequest request) {
        validateBucket();
        validateContentType(request.contentType());

        String bucket = properties.getBucket().trim();
        String key = buildObjectKey(memberId, request);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(request.contentType())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(properties.getPresignExpirationSeconds()))
                .putObjectRequest(putObjectRequest)
                .build();

        try {
            PresignedPutObjectRequest presigned = s3Presigner.presignPutObject(presignRequest);
            return new PresignUploadResponse(
                    bucket,
                    key,
                    presigned.url().toString(),
                    "PUT",
                    properties.getPresignExpirationSeconds()
            );
        } catch (RuntimeException e) {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void validateBucket() {
        if (properties.getBucket() == null || properties.getBucket().isBlank()) {
            throw new IllegalArgumentException("S3 버킷이 설정되어 있지 않습니다.");
        }
    }

    private void validateContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("contentType은 필수입니다.");
        }

        String normalized = contentType.toLowerCase(Locale.ROOT);
        if (!properties.getAllowedContentTypes().isEmpty() && !properties.getAllowedContentTypes().contains(normalized)) {
            throw new IllegalArgumentException("허용되지 않는 contentType입니다.");
        }
    }

    private String buildObjectKey(Long memberId, PresignUploadRequest request) {
        String extension = extractExtension(request.fileName());
        String date = LocalDate.now().format(DAY_FORMAT);
        String random = UUID.randomUUID().toString();
        return "%s/%d/%s/%s%s".formatted(
                request.purpose().keyPrefix(),
                memberId,
                date,
                random,
                extension
        );
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot).toLowerCase(Locale.ROOT);
    }
}
