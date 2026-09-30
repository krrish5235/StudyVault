package com.studyvault.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * Private object storage abstraction. Keys are namespaced:
 *   public/...   marketing assets (covers, previews) served through the backend
 *   private/...  paid digital files - NEVER served without entitlement checks
 */
public interface StorageService {

    /** Stores bytes at the given key and returns the key. */
    String store(String key, byte[] data, String contentType);

    InputStream open(String key);

    void delete(String key);

    boolean exists(String key);

    /**
     * Returns a short-lived signed URL, or null when the implementation has no
     * URL concept (local dev storage) and the caller must stream the content.
     */
    String presignGetUrl(String key, Duration ttl);

    static void validateKey(String key) {
        if (key == null || key.isBlank()
                || key.contains("..")
                || key.contains("\\")
                || key.startsWith("/")
                || key.contains("//")
                || key.length() > 500) {
            throw new IllegalArgumentException("Invalid storage key");
        }
    }
}
