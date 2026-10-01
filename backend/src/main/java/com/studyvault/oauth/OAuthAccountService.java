package com.studyvault.oauth;

import com.studyvault.common.audit.AuditService;
import com.studyvault.user.Role;
import com.studyvault.user.RoleRepository;
import com.studyvault.user.User;
import com.studyvault.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Finds or creates the local account for an OAuth2 identity, linking by
 * (provider, providerUserId) first and then by verified email. Auto-linking by
 * email happens ONLY when the provider attests the email is verified.
 */
@Service
@RequiredArgsConstructor
public class OAuthAccountService {

    private final OAuthAccountRepository oauthAccounts;
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;

    public record ResolvedUser(User user, boolean newlyCreated) {
    }

    @Transactional
    public ResolvedUser findOrCreate(String provider, String providerUserId, String email,
                                     String displayName, String ip) {
        Optional<OAuthAccount> existing = oauthAccounts.findByProviderAndProviderUserId(provider, providerUserId);
        if (existing.isPresent()) {
            return new ResolvedUser(existing.get().getUser(), false);
        }

        User user = users.findWithRolesByEmail(email.toLowerCase()).orElseGet(() -> {
            // Unusable random password: OAuth users cannot sign in with a password.
            String unusableHash = passwordEncoder.encode(UUID.randomUUID() + ":" + System.nanoTime());
            User created = User.register(email.toLowerCase(),
                    displayName != null && !displayName.isBlank()
                            ? displayName : email.substring(0, email.indexOf('@')),
                    unusableHash);
            created.setEmailVerified(true); // provider-verified email enforced upstream
            roles.findByName(Role.USER).ifPresent(created.getRoles()::add);
            return users.saveAndFlush(created);
        });

        OAuthAccount link = new OAuthAccount();
        link.setUser(user);
        link.setProvider(provider);
        link.setProviderUserId(providerUserId);
        oauthAccounts.save(link);

        audit.record(null, user.getEmail(), null, "AUTH_OAUTH_LINKED", "user", user.getId().toString(),
                java.util.Map.of("provider", provider));
        return new ResolvedUser(user, true);
    }
}
