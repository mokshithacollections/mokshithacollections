package com.ec.mokshitha_collections.dto.address;

import com.ec.mokshitha_collections.entity.AddressType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 50)
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50)
    private String lastName;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9+]{10,15}$", message = "Phone must be 10-15 digits, optionally starting with +")
    private String phone;

    @NotBlank(message = "Street address is required")
    @Size(max = 255)
    private String streetAddress;

    @NotBlank(message = "City is required")
    @Size(max = 100)
    private String city;

    @NotBlank(message = "State is required")
    @Size(max = 100)
    private String state;

    @NotBlank(message = "Pin code is required")
    @Pattern(regexp = "^[0-9]{4,10}$", message = "Invalid pin code")
    private String pinCode;

    @NotBlank(message = "Country is required")
    @Size(max = 100)
    private String country;

    @NotNull(message = "Address type is required")
    private AddressType addressType;

    private boolean isDefault;
}
