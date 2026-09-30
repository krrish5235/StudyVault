package com.studyvault.storage;

import com.studyvault.common.error.StorageException;
import com.studyvault.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

/**
 * Private S3 storage (also MinIO-compatible for local development). The bucket
 * must be private; paid files are only reachable through short-lived presigned URLs.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final AppProperties props;
    private S3Client s3;
    private S3Presigner presigner;
    private String bucket;

    @PostConstruct
    void init() {
        var cfg = props.getStorage().getS3();
        bucket = cfg.getBucket();
        var builder = S3Client.builder()
                .region(Region.of(cfg.getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(cfg.isPathStyleAccess())
                        .build());

        if (cfg.getEndpoint() != null && !cfg.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(cfg.getEndpoint()));
        }

        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
            builder.credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey)));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }

        this.s3 = builder.build();
        var presignerBuilder = S3Presigner.builder()
                .region(Region.of(cfg.getRegion()))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(cfg.isPathStyleAccess())
                        .build());
        if (cfg.getEndpoint() != null && !cfg.getEndpoint().isBlank()) {
            presignerBuilder.endpointOverride(URI.create(cfg.getEndpoint()));
        }
        this.presigner = presignerBuilder.build();
        log.info("S3 storage initialized (bucket={}, region={})", bucket, cfg.getRegion());
    }

    @Override
    public String store(String key, byte[] data, String contentType) {
        StorageService.validateKey(key);
        try {
            s3.putObject(PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build(), RequestBody.fromBytes(data));
            return key;
        } catch (Exception e) {
            throw new StorageException("Failed to store file", e);
        }
    }

    @Override
    public InputStream open(String key) {
        StorageService.validateKey(key);
        try {
            return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (Exception e) {
            throw new StorageException("Failed to read file", e);
        }
    }

    @Override
    public void delete(String key) {
        StorageService.validateKey(key);
        try {
            s3.deleteObject(b -> b.bucket(bucket).key(key));
        } catch (Exception e) {
            throw new StorageException("Failed to delete file", e);
        }
    }

    @Override
    public boolean exists(String key) {
        StorageService.validateKey(key);
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            throw new StorageException("Failed to check file existence", e);
        }
    }

    @Override
    public String presignGetUrl(String key, Duration ttl) {
        StorageService.validateKey(key);
        GetObjectPresignRequest presign = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build();
        return presigner.presignGetObject(presign).url().toString();
    }
}
