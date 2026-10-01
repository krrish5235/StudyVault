package com.studyvault.oauth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * OIDC counterpart of CustomOAuth2UserService. With the "openid" scope (Google),
 * Spring routes through THIS service, so the normalized attributes the success
 * handler needs are attached here. A sign-in is refused without a verified email.
 */
@Slf4j
@Component
public class CustomOidcUserService extends OidcUserService {

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        String email = oidcUser.getEmail();
        Boolean verified = oidcUser.getEmailVerified();
        if (email == null || !Boolean.TRUE.equals(verified)) {
            log.warn("OAuth sign-in refused: no verified email from provider {}", provider);
            throw new OAuth2AuthenticationException(new OAuth2Error("no_verified_email"),
                    "Your provider account has no verified email address.");
        }
        String providerId = oidcUser.getSubject();
        if (providerId == null || providerId.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("missing_provider_id"),
                    "Provider did not return an account id.");
        }
        String name = StringUtils.hasText(oidcUser.getFullName()) ? oidcUser.getFullName() : email;

        Map<String, Object> claims = new HashMap<>(oidcUser.getClaims());
        claims.put(CustomOAuth2UserService.ATTR_EMAIL, email.toLowerCase());
        claims.put(CustomOAuth2UserService.ATTR_NAME, name);
        claims.put(CustomOAuth2UserService.ATTR_PROVIDER, provider);
        claims.put(CustomOAuth2UserService.ATTR_PROVIDER_ID, providerId);
        claims.put(CustomOAuth2UserService.ATTR_VERIFIED, true);

        // Merged attributes are readable via getAttributes() in the success handler.
        return new DefaultOidcUser(oidcUser.getAuthorities(), oidcUser.getIdToken(),
                new OidcUserInfo(claims), "sub");
    }
}
