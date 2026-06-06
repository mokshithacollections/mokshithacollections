package com.ec.mokshitha_collections.service;

import com.ec.mokshitha_collections.dto.address.AddressRequest;
import com.ec.mokshitha_collections.entity.Address;
import com.ec.mokshitha_collections.entity.User;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.AddressRepository;
import com.ec.mokshitha_collections.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional
    public Address addAddress(Long userId, AddressRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (req.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        }

        Address address = Address.builder()
                .user(user)
                .firstName(req.getFirstName())
                .lastName(req.getLastName())
                .phone(req.getPhone())
                .streetAddress(req.getStreetAddress())
                .city(req.getCity())
                .state(req.getState())
                .pinCode(req.getPinCode())
                .country(req.getCountry())
                .addressType(req.getAddressType())
                .isDefault(req.isDefault())
                .build();

        return addressRepository.save(address);
    }

    @Transactional
    public void setDefaultAddress(Long userId, Long addressId) {
        Address address = loadOwned(userId, addressId);
        addressRepository.clearDefaultForUser(userId);
        address.setIsDefault(true);
        addressRepository.save(address);
    }

    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        Address address = loadOwned(userId, addressId);
        addressRepository.delete(address);
    }

    @Transactional
    public void updateAddress(Long userId, Long addressId, AddressRequest req) {
        Address existing = loadOwned(userId, addressId);

        if (req.isDefault()) {
            addressRepository.clearDefaultForUser(userId);
        }

        existing.setFirstName(req.getFirstName());
        existing.setLastName(req.getLastName());
        existing.setPhone(req.getPhone());
        existing.setStreetAddress(req.getStreetAddress());
        existing.setCity(req.getCity());
        existing.setState(req.getState());
        existing.setPinCode(req.getPinCode());
        existing.setCountry(req.getCountry());
        existing.setAddressType(req.getAddressType());
        existing.setIsDefault(req.isDefault());

        addressRepository.save(existing);
    }

    /** Loads an address by id and asserts the caller owns it; otherwise 403. */
    private Address loadOwned(Long userId, Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        if (!address.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("You do not own this address");
        }
        return address;
    }
}
