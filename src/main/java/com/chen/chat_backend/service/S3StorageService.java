package com.chen.chat_backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Service
// Stores chat images in S3-compatible storage and provides short-lived download links.
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;

    public S3StorageService(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${aws.s3.bucket}") String bucket
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
    }

    public StoredObject uploadImage(MultipartFile file, String userEmail, String sessionId) {
        requireBucket();

        String originalFileName = Objects.requireNonNullElse(file.getOriginalFilename(), "image");
        String key = buildObjectKey(userEmail, sessionId, originalFileName);

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));
            return new StoredObject(key, originalFileName, file.getContentType());
        } catch (IOException exception) {
            throw new RuntimeException("Failed to upload image to S3", exception);
        }
    }

    public String generatePresignedDownloadUrl(String key) {
        requireBucket();

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public void deleteObject(String key) {
        if (key == null || key.isBlank() || bucket == null || bucket.isBlank()) {
            return;
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        s3Client.deleteObject(request);
    }

    private void requireBucket() {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalStateException("AWS_S3_BUCKET is not configured");
        }
    }

    // Names files under a user/session path and uses a UUID to prevent collisions.
    private String buildObjectKey(String userEmail, String sessionId, String originalFileName) {
        String extension = "";
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            extension = originalFileName.substring(lastDotIndex);
        }

        String safeEmail = userEmail.replaceAll("[^a-zA-Z0-9@._-]", "_");
        return "chat-images/" + safeEmail + "/" + sessionId + "/" + UUID.randomUUID() + extension;
    }

    public record StoredObject(String key, String fileName, String contentType) {
    }
}
