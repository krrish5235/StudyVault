package com.studyvault.config;

import com.studyvault.security.CsrfDoubleSubmitFilter;
import com.studyvault.security.RateLimitFilter;
import com.studyvault.security.SessionAuthFilter;
import com.studyvault.common.web.ClientIpResolver;
import com.studyvault.common.web.JsonResponses;
import com.studyvault.user.SessionService;
import com.studyvault.oauth.CustomOAuth2UserService;
import com.studyvault.oauth.OAuthClientConfig;
import com.studyvault.oauth.OAuthLoginFailureHandler;
import com.studyvault.oauth.OAuthLoginSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Stateless security configuration: cookie session auth + CSRF double-submit +
 * rate limiting + strict headers. All authorization is enforced here and via
 * method security - never in the frontend. OAuth2 sign-in (Google/GitHub) is
 * wired only when provider credentials exist in the environment.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AppProperties props;
    private final SessionService sessionService;
    private final ClientIpResolver ipResolver;
    private final OAuthClientConfig oauthClientConfig;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuthLoginSuccessHandler oauthSuccessHandler;
    private final OAuthLoginFailureHandler oauthFailureHandler;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(props.getCors().getAllowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Accept", "X-XSRF-TOKEN", "X-Request-Id"));
        config.setExposedHeaders(List.of("X-Request-Id", "Location"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        SessionAuthFilter sessionAuthFilter = new SessionAuthFilter(sessionService, props);
        CsrfDoubleSubmitFilter csrfFilter = new CsrfDoubleSubmitFilter(props);
        RateLimitFilter rateLimitFilter = new RateLimitFilter(props, ipResolver);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable()) // replaced by CsrfDoubleSubmitFilter (SPA double-submit)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny())
                        .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'none'; frame-ancestors 'none'"))
                        .referrerPolicy(rp -> rp.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true).maxAgeInSeconds(31_536_000))
                        .permissionsPolicyHeader(pp -> pp.policy("camera=(), microphone=(), geolocation=()")))
                .exceptionHandling(eh -> eh
                        .authenticationEntryPoint((req, res, ex) ->
                                JsonResponses.write(res, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required."))
                        .accessDeniedHandler((req, res, ex) ->
                                JsonResponses.write(res, HttpServletResponse.SC_FORBIDDEN,
                                        "You do not have permission to perform this action.")))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/oauth/**", "/login/oauth2/code/*",
                                "/oauth2/authorization/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login",
                                "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                                "/api/v1/auth/verify-email").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/payments/webhook/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**",
                                "/api/v1/settings/**", "/api/v1/files/public/**").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated());

        HttpSecurity chained = http;
        if (oauthClientConfig.anyProviderEnabled()) {
            chained = http.oauth2Login(login -> login
                    // Deep baseUri so /{registrationId} cannot collide with /providers
                    .authorizationEndpoint(a -> a.baseUri("/api/v1/auth/oauth/authorize"))
                    .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                    .successHandler(oauthSuccessHandler)
                    .failureHandler(oauthFailureHandler));
        }

        return chained
                // Order: rate limit → CSRF → session auth → authorization
                .addFilterBefore(rateLimitFilter, org.springframework.security.web.csrf.CsrfFilter.class)
                .addFilterAfter(csrfFilter, org.springframework.security.web.csrf.CsrfFilter.class)
                .addFilterBefore(sessionAuthFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
