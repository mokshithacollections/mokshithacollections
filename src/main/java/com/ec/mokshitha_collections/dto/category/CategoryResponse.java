package com.ec.mokshitha_collections.dto.category;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponse {
    private final Long categoryId;
    private final String name;
    private final String slug;
    private final String description;
    private final Long parentId;
    private final Boolean isActive;
}
