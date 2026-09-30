package com.studyvault.user;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/** Issues 256-bit opaque tokens (sessions, reset, verification); only SHA-256 hashes are persisted. */
@Component
public class TokenService {

    private final SecureRandom secureRandom = new SecureRandom();

    public record Issued(String raw, String hash, Instant expiresAt) {
    }

    public Issued issue(Duration ttl) {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new Issued(raw, sha256Hex(raw), Instant.now().plus(ttl));
    }

    public static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
