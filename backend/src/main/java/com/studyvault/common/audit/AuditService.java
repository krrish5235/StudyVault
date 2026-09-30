package com.studyvault.common.audit;

import com.studyvault.common.web.ClientIpResolver;
import com.studyvault.security.SecurityUser;
import com.studyvault.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Who / what / when / which resource / result. Security- and money-relevant actions
 * must be audited. Never includes passwords, tokens or secrets in details.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository repository;
    private final ClientIpResolver ipResolver;

    public void record(HttpServletRequest request, String action, String resourceType, String resourceId,
                       Map<String, Object> details) {
        UUID actorId = SecurityUtils.currentUser().map(SecurityUser::id).orElse(null);
        String actorEmail = SecurityUtils.currentUser().map(SecurityUser::email).orElse(null);
        record(actorId, actorEmail, request, action, resourceType, resourceId, details);
    }

    public void record(UUID actorId, String actorEmail, HttpServletRequest request, String action,
                       String resourceType, String resourceId, Map<String, Object> details) {
        AuditLog entry = new AuditLog();
        entry.setActorId(actorId);
        entry.setActorEmail(actorEmail);
        entry.setAction(action);
        entry.setResourceType(resourceType);
        entry.setResourceId(resourceId);
        entry.setDetails(details);
        if (request != null) {
            entry.setIpAddress(ipResolver.resolve(request));
        }
        repository.save(entry);
    }
}
