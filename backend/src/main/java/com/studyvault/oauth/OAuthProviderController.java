package com.studyvault.oauth;

import com.studyvault.common.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Tells the frontend which OAuth buttons to render (empty when none configured). */
@RestController
@RequestMapping("/api/v1/auth/oauth")
@RequiredArgsConstructor
public class OAuthProviderController {

    private final OAuthClientConfig config;

    public record EnabledProviders(List<String> providers) {
    }

    @GetMapping("/providers")
    public ApiResponse<EnabledProviders> providers() {
        return ApiResponse.ok(new EnabledProviders(config.enabledProviders()));
    }
}
