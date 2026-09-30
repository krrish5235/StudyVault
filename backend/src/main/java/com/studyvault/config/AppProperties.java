package com.studyvault.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * All environment-driven application configuration. Values come exclusively from
 * environment variables (see .env.example / docs/ENVIRONMENT.md) - never hardcoded.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Cors cors = new Cors();
    private String frontendUrl = "http://localhost:3000";
    private Cookie cookie = new Cookie();
    private Session session = new Session();
    private Storage storage = new Storage();
    private Email email = new Email();
    private Payments payments = new Payments();
    private Razorpay razorpay = new Razorpay();
    private BootstrapAdmin bootstrapAdmin = new BootstrapAdmin();
    private RateLimit rateLimit = new RateLimit();
    private Security security = new Security();

    @Getter
    @Setter
    public static class Cors {
        /** Exact origins allowed cross-origin; empty = same-origin only. */
        private List<String> allowedOrigins = new ArrayList<>();
    }

    @Getter
    @Setter
    public static class Cookie {
        private String sessionName = "sv_session";
        private String csrfName = "sv_csrf";
        private boolean secure;
        private String domain;
    }

    @Getter
    @Setter
    public static class Session {
        private int ttlDays = 7;
        private int maxLifetimeDays = 30;
        /** Sliding expiry: extend the session when idle longer than this many hours. */
        private int slideAfterHours = 12;
    }

    @Getter
    @Setter
    public static class Storage {
        /** local (dev) | s3 */
        private String provider = "local";
        private String localDir = "./data/storage";
        private int presignTtlMinutes = 5;
        private S3 s3 = new S3();

        @Getter
        @Setter
        public static class S3 {
            private String bucket;
            private String region = "ap-south-1";
            /** Optional custom endpoint (e.g. MinIO for local development). */
            private String endpoint;
            private boolean pathStyleAccess;
        }
    }

    @Getter
    @Setter
    public static class Email {
        /** log (dev) | smtp */
        private String provider = "log";
        private String from = "no-reply@studyvault.local";
    }

    @Getter
    @Setter
    public static class Payments {
        /** razorpay | mock (mock refused in prod profile) */
        private String provider = "razorpay";
    }

    @Getter
    @Setter
    public static class Razorpay {
        private String keyId;
        private String keySecret;
        private String webhookSecret;
    }

    @Getter
    @Setter
    public static class BootstrapAdmin {
        private boolean enabled;
        private String email;
        private String password;
    }

    @Getter
    @Setter
    public static class RateLimit {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Security {
        /** Enable only when the API runs behind a trusted reverse proxy that sets X-Forwarded-For. */
        private boolean trustProxyHeaders = false;
    }
}
