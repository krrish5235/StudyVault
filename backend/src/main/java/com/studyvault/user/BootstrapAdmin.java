package com.studyvault.user;

import com.studyvault.common.audit.AuditService;
import com.studyvault.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * One-time, environment-controlled creation of the first ADMIN user
 * (ADMIN_BOOTSTRAP_ENABLED + ADMIN_BOOTSTRAP_EMAIL + ADMIN_BOOTSTRAP_PASSWORD).
 * Idempotent: does nothing once any admin exists. No default/hardcoded credentials.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapAdmin implements ApplicationRunner {

    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final AppProperties props;
    private final AuditService audit;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var config = props.getBootstrapAdmin();
        if (!config.isEnabled()) {
            return;
        }
        if (users.existsByRoleName(Role.ADMIN)) {
            log.info("Bootstrap admin skipped: an admin user already exists.");
            return;
        }
        if (isBlank(config.getEmail()) || isBlank(config.getPassword())) {
            log.error("ADMIN_BOOTSTRAP_ENABLED=true but ADMIN_BOOTSTRAP_EMAIL/ADMIN_BOOTSTRAP_PASSWORD are missing - no admin created.");
            return;
        }
        if (config.getPassword().length() < 12) {
            log.error("Bootstrap admin password must be at least 12 characters - no admin created. Fix the environment and restart.");
            return;
        }

        User admin = new User();
        admin.setEmail(config.getEmail().trim().toLowerCase());
        admin.setFullName("Administrator");
        admin.setPasswordHash(passwordEncoder.encode(config.getPassword()));
        admin.setEmailVerified(true);
        admin.setMustChangePassword(true); // forced change on first sign-in
        roles.findByName(Role.ADMIN).ifPresent(admin.getRoles()::add);
        users.save(admin);

        audit.record(admin.getId(), admin.getEmail(), null, "ADMIN_BOOTSTRAP_CREATED", "user",
                admin.getId().toString(), Map.of("email", admin.getEmail()));
        log.warn("Bootstrap admin created for {}. Change the password immediately on first sign-in, "
                + "then set ADMIN_BOOTSTRAP_ENABLED=false.", admin.getEmail());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
