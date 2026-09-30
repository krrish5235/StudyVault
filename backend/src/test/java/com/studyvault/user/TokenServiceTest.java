package com.studyvault.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTest {

    private final TokenService tokens = new TokenService();

    @Test
    void sha256IsDeterministicAndHexEncoded() {
        String first = TokenService.sha256Hex("studyvault");
        String second = TokenService.sha256Hex("studyvault");
        assertThat(first).isEqualTo(second);
        assertThat(first).hasSize(64).matches("[0-9a-f]+");
    }

    @Test
    void sha256DiffersForDifferentInputs() {
        assertThat(TokenService.sha256Hex("a")).isNotEqualTo(TokenService.sha256Hex("b"));
    }

    @Test
    void issuedTokensAreUniqueAndHashedCorrectly() {
        TokenService.Issued a = tokens.issue(java.time.Duration.ofMinutes(5));
        TokenService.Issued b = tokens.issue(java.time.Duration.ofMinutes(5));

        assertThat(a.raw()).isNotEqualTo(b.raw());
        assertThat(a.hash()).isEqualTo(TokenService.sha256Hex(a.raw()));
        assertThat(a.expiresAt()).isAfter(java.time.Instant.now());
        assertThat(a.raw()).hasSize(43); // 32 bytes, base64url unpadded
    }
}
