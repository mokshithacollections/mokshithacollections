package com.ec.mokshitha_collections.service.admin;

import com.ec.mokshitha_collections.dto.admin.UserAdminResponse;
import com.ec.mokshitha_collections.entity.User;
import com.ec.mokshitha_collections.exception.BadRequestException;
import com.ec.mokshitha_collections.exception.ResourceNotFoundException;
import com.ec.mokshitha_collections.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserAdminResponse> list(String search, Pageable pageable) {
        return userRepository.search(blankToNull(search), pageable)
                .map(AdminUserService::toResponse);
    }

    @Transactional
    public UserAdminResponse setActive(Long userId, boolean active, Long callerUserId) {
        if (!active && userId.equals(callerUserId)) {
            throw new BadRequestException("You cannot deactivate your own account");
        }
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        u.setIsActive(active);
        return toResponse(userRepository.save(u));
    }

    @Transactional
    public UserAdminResponse setAdmin(Long userId, boolean admin, Long callerUserId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!admin) {
            if (userId.equals(callerUserId)) {
                throw new BadRequestException("You cannot remove your own admin role");
            }
            // Don't let the system end up with zero admins.
            if (Boolean.TRUE.equals(u.getIsAdmin()) && userRepository.countByIsAdminTrue() <= 1) {
                throw new BadRequestException("At least one admin must remain");
            }
        }

        u.setIsAdmin(admin);
        return toResponse(userRepository.save(u));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static UserAdminResponse toResponse(User u) {
        return UserAdminResponse.builder()
                .userId(u.getUserId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .phone(u.getPhone())
                .active(Boolean.TRUE.equals(u.getIsActive()))
                .admin(Boolean.TRUE.equals(u.getIsAdmin()))
                .createdAt(u.getCreatedAt())
                .build();
    }
}
