package com.studyvault.admin;

import com.studyvault.common.api.ApiResponse;
import com.studyvault.common.audit.AuditService;
import com.studyvault.common.error.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** Site content management - copy, branding, announcement and socials, all DB-backed. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SiteSettingsController {

    private final SiteSettingsService settings;
    private final AuditService audit;

    /** Public: storefront content. */
    @GetMapping("/settings/site")
    public ApiResponse<Map<String, Object>> publicSettings() {
        return ApiResponse.ok(settings.findAllAsMap());
    }

    @PutMapping("/admin/settings/site")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Map<String, Object>> update(@RequestBody Map<String, Object> values,
                                                   HttpServletRequest httpRequest) {
        if (values == null || values.isEmpty() || values.size() > 30) {
            throw new BadRequestException("Provide between 1 and 30 settings keys.");
        }
        settings.upsertAll(values);
        audit.record(httpRequest, "SITE_SETTINGS_UPDATED", "settings", "site", Map.of("keys", values.keySet()));
        return ApiResponse.ok(settings.findAllAsMap(), "Settings saved.");
    }
}
