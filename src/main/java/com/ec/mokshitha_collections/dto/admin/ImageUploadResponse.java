package com.ec.mokshitha_collections.dto.admin;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageUploadResponse {
    private final Long imageId;
    private final String imageUrl;
    private final String viewType;
    private final boolean isPrimary;
}
