package com.ec.mokshitha_collections.controller;

import com.ec.mokshitha_collections.dto.auth.LoginRequest;
import com.ec.mokshitha_collections.dto.auth.RegisterRequest;
import com.ec.mokshitha_collections.dto.common.ApiResponse;
import com.ec.mokshitha_collections.entity.User;
import com.ec.mokshitha_collections.exception.ConflictException;
import com.ec.mokshitha_collections.exception.TooManyAttemptsException;
import com.ec.mokshitha_collections.repository.UserRepository;
import com.ec.mokshitha_collections.security.CustomUserDetails;
import com.ec.mokshitha_collections.security.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final RememberMeServices rememberMeServices;
    private final LoginAttemptService loginAttemptService;

    private final SecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();

    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(@Valid @ModelAttribute LoginRequest req,
                                             HttpServletRequest httpReq,
                                             HttpServletResponse httpRes) {

        String ip = clientIp(httpReq);

        if (loginAttemptService.isBlocked(ip)) {
            throw new TooManyAttemptsException("Too many failed attempts. Please try again later.");
        }

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (AuthenticationException ex) {
            // Both unknown email and wrong password land here — record + rethrow.
            // GlobalExceptionHandler converts to a generic 401.
            loginAttemptService.recordFailure(ip);
            throw ex;
        }

        loginAttemptService.recordSuccess(ip);

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpReq, httpRes);

        // Issue a remember-me cookie iff the user opted in.
        // PersistentTokenBasedRememberMeServices reads the "rememberMe" parameter
        // off the request to decide.
        rememberMeServices.loginSuccess(httpReq, httpRes, auth);

        CustomUserDetails principal = (CustomUserDetails) auth.getPrincipal();

        return ResponseEntity.ok(ApiResponse.builder()
                .status("success")
                .message("Login successful!")
                .data(Map.of(
                        "firstName", principal.getFirstName(),
                        "isAdmin", principal.isAdmin()))
                .build());
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @ModelAttribute RegisterRequest req) {

        String email = req.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(email).isPresent()) {
            throw new ConflictException("Email already registered");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .firstName(req.getFirstName().trim())
                .lastName(req.getLastName() != null ? req.getLastName().trim() : "")
                .phone(req.getPhone() != null && !req.getPhone().isBlank() ? req.getPhone().trim() : null)
                .isActive(true)
                .isAdmin(false)
                .build();

        User saved = userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.builder()
                .status("success")
                .message("Registration successful!")
                .data(Map.of(
                        "userId", saved.getUserId(),
                        "firstName", saved.getFirstName()))
                .build());
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(HttpServletRequest httpReq, HttpServletResponse httpRes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        new SecurityContextLogoutHandler().logout(httpReq, httpRes, auth);
        // Also delete the persistent remember-me row + cookie.
        rememberMeServices.loginFail(httpReq, httpRes);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @GetMapping("/check-session")
    public ResponseEntity<Map<String, Object>> checkSession() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof CustomUserDetails p)) {
            return ResponseEntity.ok(Map.of("loggedIn", false));
        }
        return ResponseEntity.ok(Map.of(
                "loggedIn", true,
                "userId", p.getUserId(),
                "email", p.getEmail(),
                "firstName", p.getFirstName(),
                "isAdmin", p.isAdmin()));
    }

    /**
     * Best-effort client IP. With server.forward-headers-strategy=framework
     * Spring Boot already substitutes X-Forwarded-For for us when we sit
     * behind a trusted reverse proxy.
     */
    private String clientIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        if (fwd != null && !fwd.isBlank()) {
            return fwd.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }
}
