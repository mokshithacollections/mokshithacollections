package com.ec.mokshitha_collections.dto.contact;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Payload from the public contact form (POST /api/contact). */
@Data
public class ContactRequest {

    @NotBlank @Size(max = 120)
    private String name;

    @NotBlank @Email @Size(max = 160)
    private String email;

    @Size(max = 30)
    private String phone;

    @Size(max = 60)
    private String subject;

    @NotBlank @Size(max = 5000)
    private String message;

    private Boolean newsletter;
}
