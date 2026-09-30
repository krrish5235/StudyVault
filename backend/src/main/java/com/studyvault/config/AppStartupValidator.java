package com.studyvault.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fails startup in the prod profile when security-critical configuration is missing
 * or unsafe. Development defaults must never leak into production.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppStartupValidator implements ApplicationRunner {

    private final AppProperties props;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        List<String> active = List.of(environment.getActiveProfiles());
        if (!active.contains("prod")) {
            return;
        }
        List<String> problems = new ArrayList<>();

        if (!props.getCookie().isSecure()) {
            problems.add("APP_COOKIE_SECURE must be true in production (session cookies without Secure flag)");
        }
        if (props.getCors().getAllowedOrigins().isEmpty()) {
            problems.add("APP_CORS_ALLOWED_ORIGINS must list the allowed frontend origins in production");
        }
        if ("s3".equals(props.getStorage().getProvider()) && isBlank(props.getStorage().getS3().getBucket())) {
            problems.add("AWS_S3_BUCKET is required when STORAGE_PROVIDER=s3");
        }
        if ("mock".equals(props.getPayments().getProvider())) {
            problems.add("PAYMENT_PROVIDER=mock is not allowed in production");
        }
        if ("razorpay".equals(props.getPayments().getProvider())
                && (isBlank(props.getRazorpay().getKeyId()) || isBlank(props.getRazorpay().getKeySecret()))) {
            problems.add("RAZORPAY_KEY_ID / RAZORPAY_KEY_SECRET are required in production");
        }
        if ("smtp".equals(props.getEmail().getProvider()) && isBlank(props.getEmail().getFrom())) {
            problems.add("EMAIL_FROM is required when EMAIL_PROVIDER=smtp");
        }
        if (props.getBootstrapAdmin().isEnabled() && !isBlank(props.getBootstrapAdmin().getPassword())
                && props.getBootstrapAdmin().getPassword().length() < 12) {
            problems.add("ADMIN_BOOTSTRAP_PASSWORD must be at least 12 characters");
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException("Refusing to start with unsafe production configuration: "
                    + String.join("; ", problems));
        }
        log.info("Production configuration validated ({} checks passed).", problems.size() + 1);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
