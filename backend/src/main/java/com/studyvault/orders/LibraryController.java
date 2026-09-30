package com.studyvault.orders;

import com.studyvault.catalog.Product;
import com.studyvault.catalog.ProductFile;
import com.studyvault.catalog.ProductFileRepository;
import com.studyvault.common.error.BadRequestException;
import com.studyvault.common.error.ForbiddenException;
import com.studyvault.common.error.NotFoundException;
import com.studyvault.common.web.ClientIpResolver;
import com.studyvault.config.AppProperties;
import com.studyvault.security.SecurityUser;
import com.studyvault.security.SecurityUtils;
import com.studyvault.user.User;
import com.studyvault.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * The purchased library. Every download re-verifies the entitlement server-side
 * and is logged; paid bytes leave storage only via short-lived presigned URLs
 * (S3) or an authenticated stream (local dev storage).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/library")
@RequiredArgsConstructor
public class LibraryController {

    private final EntitlementRepository entitlements;
    private final ProductFileRepository files;
    private final UserRepository users;
    private final DownloadRepository downloads;
    private final com.studyvault.storage.StorageService storage;
    private final ClientIpResolver ipResolver;
    private final AppProperties props;

    public record LibraryItem(UUID productId, String title, String slug, String categoryName,
                              String coverImageKey, int contentVersion, Instant grantedAt,
                              boolean downloadable) {
    }

    @GetMapping
    public List<LibraryItem> library() {
        SecurityUser su = SecurityUtils.requireUser();
        return entitlements.findAllByUserIdAndRevokedAtIsNullOrderByGrantedAtDesc(su.id()).stream()
                .map(e -> {
                    Product p = e.getProduct();
                    boolean downloadable = files.findByProductIdAndCurrentTrue(p.getId()).isPresent();
                    return new LibraryItem(p.getId(), p.getTitle(), p.getSlug(), p.getCategory().getName(),
                            p.getCoverImageKey(), p.getContentVersion(), e.getGrantedAt(), downloadable);
                })
                .toList();
    }

    @GetMapping("/{productId}/download")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> download(
            @PathVariable UUID productId, HttpServletRequest request) {
        SecurityUser su = SecurityUtils.requireUser();
        Entitlement entitlement = entitlements
                .findByUserIdAndProductIdAndRevokedAtIsNull(su.id(), productId)
                .orElseThrow(() -> new ForbiddenException("You do not have access to this product."));

        ProductFile file = files.findByProductIdAndCurrentTrue(productId)
                .orElseThrow(() -> new NotFoundException("This product has no downloadable file yet."));

        // Log every download (who/when/what/from where).
        Download record = new Download();
        record.setUser(users.getReferenceById(su.id()));
        record.setProduct(entitlement.getProduct());
        record.setFile(file);
        record.setIpAddress(ipResolver.resolve(request));
        downloads.save(record);

        Duration ttl = Duration.ofMinutes(props.getStorage().getPresignTtlMinutes());
        String presigned = storage.presignGetUrl(file.getStorageKey(), ttl);
        if (presigned != null) {
            throw new PresignedRedirect(presigned);
        }

        // Local dev storage: stream through the backend (same authorization semantics).
        String filename = sanitizeFilename(file.getOriginalFilename(),
                entitlement.getProduct().getSlug() + "." + extOf(file.getContentType()));
        InputStream in = storage.open(file.getStorageKey());
        MediaType mediaType = file.getContentType().startsWith("image/") ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(file.getContentType());
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(new org.springframework.core.io.InputStreamResource(in));
    }

    /** Carries the presigned URL out of the controller for a 302 redirect. */
    public static class PresignedRedirect extends RuntimeException {
        public PresignedRedirect(String url) {
            super(url);
        }
    }

    @ExceptionHandler(PresignedRedirect.class)
    public ResponseEntity<Void> redirect(PresignedRedirect ex) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, ex.getMessage())
                .build();
    }

    private String sanitizeFilename(String original, String fallback) {
        if (original == null || original.isBlank()) {
            return fallback;
        }
        String safe = new java.io.File(original.replace('\\', '/')).getName().replaceAll("[^A-Za-z0-9._-]", "_");
        return safe.isBlank() ? fallback : safe;
    }

    private String extOf(String contentType) {
        return switch (contentType) {
            case "application/pdf" -> "pdf";
            case "application/zip" -> "zip";
            default -> "bin";
        };
    }
}
