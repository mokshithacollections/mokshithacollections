package com.ec.mokshitha_collections.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

/**
 * Uniform success/info response envelope used by all REST endpoints.
 * Fields with null values are omitted from the JSON to keep payloads small.
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse {
    private final String status;   // "success" | "error"
    private final String message;
    private final Map<String, Object> data;

    public static ApiResponse success(String message) {
        return ApiResponse.builder().status("success").message(message).build();
    }

    public static ApiResponse success(String message, Map<String, Object> data) {
        return ApiResponse.builder().status("success").message(message).data(data).build();
    }

    public static ApiResponse error(String message) {
        return ApiResponse.builder().status("error").message(message).build();
    }
}
