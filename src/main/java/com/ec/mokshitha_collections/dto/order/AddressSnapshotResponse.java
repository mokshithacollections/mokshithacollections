package com.ec.mokshitha_collections.dto.order;

import com.ec.mokshitha_collections.entity.AddressType;
import com.ec.mokshitha_collections.entity.DeliveryMethod;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressSnapshotResponse {
    private final String firstName;
    private final String lastName;
    private final String phone;
    private final String streetAddress;
    private final String city;
    private final String district;
    private final String state;
    private final String pinCode;
    private final String country;
    private final AddressType addressType;
    private final DeliveryMethod deliveryMethod;
}
