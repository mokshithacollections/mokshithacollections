package com.ec.mokshitha_collections.security;

import com.ec.mokshitha_collections.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapter exposing only what Spring Security needs from the User entity.
 * Carries userId / email / firstName for downstream controllers via
 * @AuthenticationPrincipal — never the password hash beyond auth.
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long userId;
    private final String email;
    private final String firstName;
    private final String passwordHash;
    private final boolean active;
    private final boolean admin;

    public CustomUserDetails(User user) {
        this.userId = user.getUserId();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.passwordHash = user.getPasswordHash();
        this.active = Boolean.TRUE.equals(user.getIsActive());
        this.admin = Boolean.TRUE.equals(user.getIsAdmin());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return admin
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"),
                          new SimpleGrantedAuthority("ROLE_USER"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override public String getPassword()           { return passwordHash; }
    @Override public String getUsername()           { return email; }
    @Override public boolean isAccountNonExpired()  { return true; }
    @Override public boolean isAccountNonLocked()   { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()            { return active; }
}
