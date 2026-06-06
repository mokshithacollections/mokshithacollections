package com.ec.mokshitha_collections.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 254, message = "Email is too long")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Password must contain at least one letter and one number")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name is too long")
    private String firstName;

    @Size(max = 50, message = "Last name is too long")
    private String lastName;

    @Pattern(
            regexp = "^$|^[0-9+]{10,15}$",
            message = "Phone must be 10-15 digits, optionally starting with +")
    private String phone;
}
