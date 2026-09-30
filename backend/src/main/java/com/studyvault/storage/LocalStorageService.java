package com.studyvault.storage;

import com.studyvault.config.AppProperties;
import com.studyvault.common.error.StorageException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;

/** Development storage: files live in a private local directory outside the web root. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final AppProperties props;
    private Path baseDir;

    @PostConstruct
    void init() {
        baseDir = Path.of(props.getStorage().getLocalDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new StorageException("Could not initialize local storage", e);
        }
        log.info("Local storage initialized at {}", baseDir);
    }

    private Path resolve(String key) {
        StorageService.validateKey(key);
        Path path = baseDir.resolve(key).normalize();
        if (!path.startsWith(baseDir)) {
            throw new StorageException("Invalid storage key");
        }
        return path;
    }

    @Override
    public String store(String key, byte[] data, String contentType) {
        Path path = resolve(key);
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, data, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            return key;
        } catch (IOException e) {
            throw new StorageException("Failed to store file", e);
        }
    }

    @Override
    public InputStream open(String key) {
        try {
            return Files.newInputStream(resolve(key), StandardOpenOption.READ);
        } catch (IOException e) {
            throw new StorageException("Failed to read file", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new StorageException("Failed to delete file", e);
        }
    }

    @Override
    public boolean exists(String key) {
        return Files.exists(resolve(key));
    }

    @Override
    public String presignGetUrl(String key, Duration ttl) {
        return null; // content is streamed through the backend after authorization
    }
}
