package com.studyvault.catalog;

import com.studyvault.common.audit.AuditService;
import com.studyvault.common.error.BadRequestException;
import com.studyvault.common.error.NotFoundException;
import com.studyvault.security.SecurityUtils;
import com.studyvault.storage.FileValidationService;
import com.studyvault.storage.StorageService;
import com.studyvault.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin file management. Digital files get versioned uploads (previous versions are
 * retained for rollback); cover/preview images are served publicly from the public/ namespace.
 */
@RestController
@RequestMapping("/api/v1/admin/products/{productId}/files")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFileController {

    private final ProductRepository products;
    private final ProductFileRepository files;
    private final ProductPreviewRepository previews;
    private final UserRepository users;
    private final StorageService storage;
    private final FileValidationService validator;
    private final AuditService audit;

    public record ProductFileDto(UUID id, int version, String originalFilename, String contentType,
                                 long sizeBytes, boolean current, String uploadedAt) {
        public static ProductFileDto from(ProductFile f) {
            return new ProductFileDto(f.getId(), f.getVersion(), f.getOriginalFilename(), f.getContentType(),
                    f.getSizeBytes(), f.isCurrent(), f.getUploadedAt().toString());
        }
    }

    /** Uploads a new digital file version; previous versions are kept for rollback. */
    @PostMapping("/digital")
    @Transactional
    public ProductFileDto uploadDigital(@PathVariable UUID productId,
                                        @RequestParam("file") MultipartFile file,
                                        HttpServletRequest httpRequest) {
        Product product = requireProduct(productId);
        FileValidationService.ValidatedFile vf = validator.validateDigital(file);

        int nextVersion = files.findAllByProductIdOrderByVersionDesc(productId).stream()
                .mapToInt(ProductFile::getVersion)
                .max().orElse(0) + 1;

        String key = FileValidationService.randomKey(
                "private/products/" + productId + "/files/v" + nextVersion + "-", vf.extension());
        storage.store(key, vf.data(), vf.contentType());

        files.clearCurrentForProduct(productId);
        ProductFile pf = new ProductFile();
        pf.setProduct(product);
        pf.setVersion(nextVersion);
        pf.setStorageKey(key);
        pf.setOriginalFilename(vf.safeOriginalName());
        pf.setContentType(vf.contentType());
        pf.setSizeBytes((long) vf.data().length);
        pf.setCurrent(true);
        SecurityUtils.currentUser().flatMap(u -> users.findById(u.id())).ifPresent(pf::setUploadedBy);
        ProductFile saved = files.saveAndFlush(pf);

        audit.record(httpRequest, "PRODUCT_FILE_UPLOADED", "product", productId.toString(),
                Map.of("version", nextVersion, "sizeBytes", pf.getSizeBytes(), "filename", vf.safeOriginalName()));
        return ProductFileDto.from(saved);
    }

    /** Rolls the active digital file back to a previous version. */
    @PostMapping("/{fileId}/activate")
    @Transactional
    public ProductFileDto activateVersion(@PathVariable UUID productId, @PathVariable UUID fileId,
                                          HttpServletRequest httpRequest) {
        requireProduct(productId);
        ProductFile target = files.findById(fileId)
                .filter(f -> f.getProduct().getId().equals(productId))
                .orElseThrow(() -> new NotFoundException("File version not found."));
        files.clearCurrentForProduct(productId);
        target.setCurrent(true);
        files.save(target);
        audit.record(httpRequest, "PRODUCT_FILE_ACTIVATED", "product", productId.toString(),
                Map.of("version", target.getVersion()));
        return ProductFileDto.from(target);
    }

    @GetMapping
    public List<ProductFileDto> listVersions(@PathVariable UUID productId) {
        requireProduct(productId);
        return files.findAllByProductIdOrderByVersionDesc(productId).stream()
                .map(ProductFileDto::from)
                .toList();
    }

    @PostMapping(value = "/cover", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Map<String, String> uploadCover(@PathVariable UUID productId,
                                           @RequestParam("file") MultipartFile file,
                                           HttpServletRequest httpRequest) {
        Product product = requireProduct(productId);
        FileValidationService.ValidatedFile vf = validator.validateImage(file);
        String key = FileValidationService.randomKey("public/products/" + productId + "/cover-", vf.extension());
        storage.store(key, vf.data(), vf.contentType());
        product.setCoverImageKey(key);
        products.save(product);
        audit.record(httpRequest, "PRODUCT_COVER_UPLOADED", "product", productId.toString(), Map.of());
        return Map.of("coverImageKey", key);
    }

    @PostMapping(value = "/previews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public Map<String, Object> uploadPreview(@PathVariable UUID productId,
                                             @RequestParam("file") MultipartFile file,
                                             HttpServletRequest httpRequest) {
        Product product = requireProduct(productId);
        FileValidationService.ValidatedFile vf = validator.validateImage(file);
        long count = previews.findAllByProductIdOrderBySortOrderAsc(productId).size();
        if (count >= 6) {
            throw new BadRequestException("A product can have at most 6 preview images.");
        }
        String key = FileValidationService.randomKey("public/products/" + productId + "/preview-", vf.extension());
        storage.store(key, vf.data(), vf.contentType());
        ProductPreview preview = new ProductPreview();
        preview.setProduct(product);
        preview.setStorageKey(key);
        preview.setSortOrder((int) count);
        previews.save(preview);
        audit.record(httpRequest, "PRODUCT_PREVIEW_UPLOADED", "product", productId.toString(), Map.of());
        return Map.of("storageKey", key);
    }

    private Product requireProduct(UUID productId) {
        return products.findWithRelationsById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found."));
    }
}
