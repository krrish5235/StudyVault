package com.studyvault.security;

import com.studyvault.common.web.ClientIpResolver;
import com.studyvault.config.AppProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Route-class-aware rate limiting with an in-memory token bucket per
 * (route rule, client). Single-instance only - switch to a shared store
 * (e.g. Redis) before horizontally scaling the API.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    /** Ordered rules; first match wins. */
    public record Rule(String pattern, int perMinute) {}

    public static final List<Rule> RULES = List.of(
            new Rule("/api/v1/auth/login", 10),
            new Rule("/api/v1/auth/register", 5),
            new Rule("/api/v1/auth/forgot-password", 3),
            new Rule("/api/v1/auth/reset-password", 5),
            new Rule("/api/v1/auth/resend-verification", 3),
            new Rule("/api/v1/payments/verify", 20),
            new Rule("/api/v1/payments/orders", 10),
            new Rule("/api/v1/library/*/download", 30),
            new Rule("/api/v1/admin/**", 120),
            new Rule("/api/**", 240)
    );

    private static final long NANOS_PER_MINUTE = 60_000_000_000L;
    private static final long IDLE_EVICT_NANOS = 3_600_000_000_000L;

    private final AppProperties props;
    private final ClientIpResolver ipResolver;
    private final AntPathMatcher matcher = new AntPathMatcher();
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final AtomicInteger acquireCount = new AtomicInteger();

    public RateLimitFilter(AppProperties props, ClientIpResolver ipResolver) {
        this.props = props;
        this.ipResolver = ipResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !props.getRateLimit().isEnabled() || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        Rule rule = RULES.stream().filter(r -> matcher.match(r.pattern(), path)).findFirst().orElse(null);
        boolean allowed = rule == null
                || tryAcquire(ruleKey(rule, path) + "|" + ipResolver.resolve(request), rule.perMinute());
        if (!allowed) {
            response.addHeader("Retry-After", "60");
            com.studyvault.common.web.JsonResponses.write(response, 429,
                    "Too many requests. Please slow down and try again shortly.");
            return;
        }
        chain.doFilter(request, response);
    }

    private String ruleKey(Rule rule, String path) {
        // Key by rule + literal path segment pattern so distinct endpoints get distinct buckets.
        return rule.pattern();
    }

    /** Package-visible for tests and per-key limits (e.g. per-email throttling). */
    public boolean tryAcquire(String key, int perMinute) {
        maybeEvict();
        long now = System.nanoTime();
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(now, perMinute));
        synchronized (bucket) {
            double refill = ((double) (now - bucket.lastNanos) / NANOS_PER_MINUTE) * bucket.capacity;
            bucket.tokens = Math.min(bucket.capacity, bucket.tokens + refill);
            bucket.lastNanos = now;
            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                return true;
            }
            return false;
        }
    }

    private void maybeEvict() {
        if (acquireCount.incrementAndGet() % 8192 != 0) {
            return;
        }
        long cutoff = System.nanoTime() - IDLE_EVICT_NANOS;
        buckets.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().lastNanos < cutoff;
            }
        });
    }

    private static final class Bucket {
        private double tokens;
        private final double capacity;
        private long lastNanos;

        private Bucket(long nowNanos, int perMinute) {
            this.capacity = Math.max(1, perMinute);
            this.tokens = capacity; // start full
            this.lastNanos = nowNanos;
        }
    }
}
