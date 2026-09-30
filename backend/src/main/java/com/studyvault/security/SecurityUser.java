package com.studyvault.security;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;
import java.util.UUID;

/** Authenticated principal placed in the SecurityContext by SessionAuthFilter. */
public record SecurityUser(UUID id, String email, String fullName,
                           Collection<? extends GrantedAuthority> authorities) {
}
