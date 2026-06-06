package com.ec.mokshitha_collections.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryCreateRequest {

    @NotBlank @Size(max = 100)
    private String name;

    @NotBlank @Size(max = 100)
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$",
             message = "Slug must be lowercase letters/numbers separated by hyphens")
    private String slug;

    private String description;

    private Long parentId;

    private Boolean isActive;
}
