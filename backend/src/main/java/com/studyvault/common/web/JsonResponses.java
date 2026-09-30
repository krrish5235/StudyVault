package com.studyvault.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyvault.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Writes JSON error bodies directly from filters / security handlers. */
public final class JsonResponses {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonResponses() {
    }

    public static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        MAPPER.writeValue(response.getWriter(), ApiResponse.error(message));
    }
}
