package com.ec.mokshitha_collections.dto.enquiry;

import com.ec.mokshitha_collections.entity.EnquiryStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnquiryResponse {
    private final Long enquiryId;
    private final String name;
    private final String email;
    private final String phone;
    private final String subject;
    private final String message;
    private final Boolean newsletter;
    private final EnquiryStatus status;
    private final LocalDateTime createdAt;
}
