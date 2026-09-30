package com.studyvault.security;

import com.studyvault.common.web.ClientIpResolver;
import com.studyvault.config.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private AppProperties props;
    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        props = new AppProperties();
        filter = new RateLimitFilter(props, new ClientIpResolver(props));
    }

    private boolean call(String path, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", path);
        request.setRemoteAddr(ip);
        return filter.tryAcquire(RateLimitFilter.RULES.stream()
                .filter(r -> new org.springframework.util.AntPathMatcher().match(r.pattern(), path))
                .findFirst()
                .map(r -> r.pattern() + "|" + ip)
                .orElse("default|" + ip), RateLimitFilter.RULES.stream()
                .filter(r -> new org.springframework.util.AntPathMatcher().match(r.pattern(), path))
                .findFirst()
                .map(RateLimitFilter.Rule::perMinute)
                .orElse(240));
    }

    @Test
    void allowsTrafficWithinLimitAndBlocksBeyond() {
        String ip = "10.0.0.1";
        // login rule: 10/min
        for (int i = 0; i < 10; i++) {
            assertThat(call("/api/v1/auth/login", ip)).as("attempt %d", i + 1).isTrue();
        }
        assertThat(call("/api/v1/auth/login", ip)).isFalse();
    }

    @Test
    void limitsArePerClient() {
        for (int i = 0; i < 10; i++) {
            call("/api/v1/auth/register", "10.0.0.2");
        }
        assertThat(call("/api/v1/auth/register", "10.0.0.2")).isFalse();
        assertThat(call("/api/v1/auth/register", "10.0.0.3")).isTrue();
    }

    @Test
    void differentEndpointsHaveIndependentBuckets() {
        for (int i = 0; i < 10; i++) {
            call("/api/v1/auth/login", "10.0.0.4");
        }
        assertThat(call("/api/v1/auth/login", "10.0.0.4")).isFalse();
        assertThat(call("/api/v1/products", "10.0.0.4")).isTrue();
    }

    @Test
    void filterSkipsNonApiPaths() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/some-page");
        MockHttpServletResponse response = new MockHttpServletResponse();
        jakarta.servlet.FilterChain chain = org.mockito.Mockito.mock(jakarta.servlet.FilterChain.class);
        filter.doFilter(request, response, chain);
        org.mockito.Mockito.verify(chain).doFilter(request, response);
    }
}
