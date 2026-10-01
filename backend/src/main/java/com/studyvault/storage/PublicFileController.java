package com.studyvault.storage;

import com.studyvault.common.error.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.HandlerMapping;

import java.io.InputStream;
import java.time.Duration;

/**
 * Serves PUBLIC marketing assets (covers, preview images).
 * URL contract: GET /api/v1/files/public/{key-without-the-public-prefix}
 * e.g. storage key "public/products/<id>/cover-x.png" is served at
 * /api/v1/files/public/products/<id>/cover-x.png.
 * Anything outside the public namespace (paid files under private/) is never reachable here.
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class PublicFileController {

    private final StorageService storage;

    @GetMapping("/public/**")
    public ResponseEntity<byte[]> servePublic(jakarta.servlet.http.HttpServletRequest request) {
        // Extract from the request URI directly - reliable across path matcher strategies.
        String uri = request.getRequestURI();
        String prefix = "/api/v1/files/public/";
        if (uri == null || !uri.startsWith(prefix)) {
            throw new NotFoundException("File not found.");
        }
        String rest = java.net.URLDecoder.decode(uri.substring(prefix.length()), java.nio.charset.StandardCharsets.UTF_8);
        if (rest.isBlank() || rest.startsWith("/")) {
            throw new NotFoundException("File not found.");
        }
        // Namespace allowlist - defense in depth against path games.
        if (!rest.startsWith("products/") || rest.contains("..")) {
            throw new NotFoundException("File not found.");
        }
        String key = "public/" + rest;
        if (!storage.exists(key)) {
            throw new NotFoundException("File not found.");
        }

        String contentType = contentTypeFor(key);
        try (InputStream in = storage.open(key)) {
            byte[] data = in.readAllBytes();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.length))
                    .body(data);
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new NotFoundException("File not found.");
        }
    }

    private String contentTypeFor(String key) {
        String lower = key.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }
}
