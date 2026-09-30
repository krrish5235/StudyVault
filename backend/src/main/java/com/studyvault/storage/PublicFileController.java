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
import java.util.Arrays;

/**
 * Serves PUBLIC marketing assets (covers, preview images) from the public/ namespace.
 * Paid files live under private/ and are never reachable through this endpoint.
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class PublicFileController {

    private final StorageService storage;

    @GetMapping("/public/**")
    public ResponseEntity<byte[]> servePublic(jakarta.servlet.http.HttpServletRequest request) {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String key = path != null ? path.replaceFirst("^/api/v1/files/public/", "") : "";
        if (!key.startsWith("public/")) {
            throw new NotFoundException("File not found.");
        }
        // Defense in depth: only a strict allowlist of public namespaces is servable.
        String namespace = key.split("/")[1];
        if (!Arrays.asList("products").contains(namespace)) {
            throw new NotFoundException("File not found.");
        }
        if (!storage.exists(key)) {
            throw new NotFoundException("File not found.");
        }

        String contentType = contentTypeFor(key);
        try (InputStream in = storage.open(key)) {
            byte[] data = in.readAllBytes();
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)).cachePublic())
                    .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(data.length))
                    .body(data);
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
