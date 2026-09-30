package com.studyvault.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** Consistent API response envelope for every endpoint. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, T data, String message, Map<String, String> errors) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    public static ApiResponse<Void> message(String message) {
        return new ApiResponse<>(true, null, message, null);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, null, message, null);
    }

    public static ApiResponse<Void> error(String message, Map<String, String> fieldErrors) {
        return new ApiResponse<>(false, null, message, fieldErrors);
    }
}
