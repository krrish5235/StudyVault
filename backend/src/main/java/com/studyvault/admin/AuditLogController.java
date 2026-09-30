package com.studyvault.admin;

import com.studyvault.common.audit.AuditLog;
import com.studyvault.common.audit.AuditLogRepository;
import com.studyvault.common.api.ApiResponse;
import com.studyvault.common.api.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogRepository auditLogs;

    public record AuditLogDto(Instant createdAt, String actorEmail, String action,
                              String resourceType, String resourceId, Map<String, Object> details,
                              String ipAddress) {
    }

    @GetMapping
    public ApiResponse<PageResponse<AuditLogDto>> list(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "30") int size) {
        Page<AuditLog> result = auditLogs.findAllByOrderByCreatedAtDesc(
                PageRequest.of(Math.max(0, page), Math.min(100, Math.max(1, size))));
        return ApiResponse.ok(PageResponse.of(result, result.map(this::toDto).getContent()));
    }

    private AuditLogDto toDto(AuditLog log) {
        return new AuditLogDto(log.getCreatedAt(), log.getActorEmail(), log.getAction(),
                log.getResourceType(), log.getResourceId(), log.getDetails(), log.getIpAddress());
    }
}
