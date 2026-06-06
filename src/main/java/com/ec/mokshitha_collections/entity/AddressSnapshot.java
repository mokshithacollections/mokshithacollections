package com.ec.mokshitha_collections.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A frozen copy of an Address taken at the time an Order is placed. Stored
 * inline on the Order so order history stays accurate even if the user
 * later edits or deletes the source address.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressSnapshot {

    @Column(name = "ship_first_name",  nullable = false)              private String firstName;
    @Column(name = "ship_last_name",   nullable = false)              private String lastName;
    @Column(name = "ship_phone",       nullable = false)              private String phone;
    @Column(name = "ship_street",      nullable = false)              private String streetAddress;
    @Column(name = "ship_city",        nullable = false)              private String city;
    @Column(name = "ship_state",       nullable = false)              private String state;
    @Column(name = "ship_pin_code",    nullable = false)              private String pinCode;
    @Column(name = "ship_country",     nullable = false)              private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "ship_address_type", nullable = false)
    private AddressType addressType;

    public static AddressSnapshot from(Address address) {
        return AddressSnapshot.builder()
                .firstName(address.getFirstName())
                .lastName(address.getLastName())
                .phone(address.getPhone())
                .streetAddress(address.getStreetAddress())
                .city(address.getCity())
                .state(address.getState())
                .pinCode(address.getPinCode())
                .country(address.getCountry())
                .addressType(address.getAddressType())
                .build();
    }
}
