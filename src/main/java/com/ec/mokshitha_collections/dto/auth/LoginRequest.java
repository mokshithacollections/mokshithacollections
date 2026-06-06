package com.ec.mokshitha_collections.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 1, max = 128, message = "Password length is invalid")
    private String password;

    /** Optional — when true, server issues a long-lived remember-me cookie. */
    private boolean rememberMe;
}
