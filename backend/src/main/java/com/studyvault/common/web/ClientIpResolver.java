package com.studyvault.common.web;

import com.studyvault.config.AppProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Client IP resolution. X-Forwarded-For is only honored when the API runs behind a
 * trusted proxy (app.security.trust-proxy-headers=true); otherwise it is attacker-
 * controllable and the direct connection address is used.
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private final AppProperties props;

    public String resolve(HttpServletRequest request) {
        if (props.getSecurity().isTrustProxyHeaders()) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                // The last element is added by the closest trusted proxy and is the
                // spoof-resistant choice.
                String[] parts = xff.split(",");
                String candidate = parts[parts.length - 1].trim();
                if (!candidate.isEmpty()) {
                    return truncate(candidate);
                }
            }
            String real = request.getHeader("X-Real-IP");
            if (real != null && !real.isBlank()) {
                return truncate(real.trim());
            }
        }
        return truncate(request.getRemoteAddr());
    }

    private String truncate(String ip) {
        return ip.length() > 45 ? ip.substring(0, 45) : ip;
    }
}
