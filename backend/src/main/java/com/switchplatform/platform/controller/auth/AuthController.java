package com.switchplatform.platform.controller.auth;

import com.switchplatform.platform.config.auth.JwtUtil;
import com.switchplatform.platform.model.auth.AuditLog;
import com.switchplatform.platform.model.auth.AuthUser;
import com.switchplatform.platform.service.auth.AuditService;
import com.switchplatform.platform.service.auth.AuthUserService;
import com.switchplatform.platform.service.auth.MfaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AuthUserService authUserService;
    private final MfaService mfaService;
    private final AuditService auditService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        if (authUserService.isAccountLocked(request.getUsername())) {
            auditService.record("LOGIN", "AUTH", request.getUsername(), "Account locked", "DENIED", request.getUsername(), null, servletRequest);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Account locked. Try again later."));
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            authUserService.recordFailedAttempt(request.getUsername());
            auditService.record("LOGIN", "AUTH", request.getUsername(), "Invalid credentials", "DENIED", request.getUsername(), null, servletRequest);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }

        authUserService.resetFailedAttempts(request.getUsername());
        AuthUser user = authUserService.findByUsername(request.getUsername());

        if (user.isMfaEnabled()) {
            auditService.record("LOGIN", "AUTH", request.getUsername(), "MFA required", "MFA_REQUIRED", request.getUsername(), user.getId(), servletRequest);
            return ResponseEntity.ok(Map.of("mfaRequired", true, "username", user.getUsername()));
        }

        authUserService.recordLogin(request.getUsername());
        auditService.record("LOGIN", "AUTH", request.getUsername(), "Login successful", "SUCCESS", request.getUsername(), user.getId(), servletRequest);

        String accessToken = jwtUtil.generateAccessToken(
                user.getUsername(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        return ResponseEntity.ok(new AuthResponse(
                accessToken, refreshToken,
                user.getUsername(), user.getRole().name(),
                user.getDisplayName(), user.getEmail()));
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<?> setupMfa(@RequestBody Map<String, String> body) {
        String username = resolveUsername(body.get("username"));
        if (username == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        try {
            MfaService.MfaSetupData data = mfaService.setupMfa(username);
            auditService.record("ENABLE_MFA", "AUTH", username, "MFA setup initiated", "SUCCESS", username, resolveUserId(username), null);
            return ResponseEntity.ok(Map.of("secret", data.secret(), "uri", data.uri()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<?> verifyMfa(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String code = body.get("code");
        if (username == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and code required"));
        }
        boolean verified = mfaService.verifyAndEnable(username, code);
        if (!verified) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid verification code"));
        }
        auditService.record("ENABLE_MFA", "AUTH", username, "MFA enabled", username, resolveUserId(username));
        return ResponseEntity.ok(Map.of("enabled", true));
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<?> disableMfa(@RequestBody Map<String, String> body) {
        String username = resolveUsername(body.get("username"));
        String code = body.get("code");
        if (username == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and code required"));
        }
        boolean disabled = mfaService.disableMfa(username, code);
        if (!disabled) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid verification code"));
        }
        auditService.record("DISABLE_MFA", "AUTH", username, "MFA disabled", username, resolveUserId(username));
        return ResponseEntity.ok(Map.of("disabled", true));
    }

    @PostMapping("/mfa/authenticate")
    public ResponseEntity<?> authenticateMfa(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String code = body.get("code");
        if (username == null || code == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "username and code required"));
        }
        if (!mfaService.validateCode(username, code)) {
            auditService.record("VERIFY_MFA", "AUTH", username, "MFA code invalid", "DENIED", username, resolveUserId(username), null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Invalid MFA code"));
        }

        AuthUser user = authUserService.findByUsername(username);
        authUserService.recordLogin(username);
        auditService.record("VERIFY_MFA", "AUTH", username, "MFA verified", username, user.getId());

        String accessToken = jwtUtil.generateAccessToken(
                user.getUsername(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        return ResponseEntity.ok(new AuthResponse(
                accessToken, refreshToken,
                user.getUsername(), user.getRole().name(),
                user.getDisplayName(), user.getEmail()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletRequest servletRequest) {
        try {
            AuthUser.Role role = request.getRole() != null
                    ? AuthUser.Role.valueOf(request.getRole().toUpperCase())
                    : AuthUser.Role.OPERATOR;

            AuthUser user = authUserService.registerUser(
                    request.getUsername(), request.getPassword(),
                    request.getEmail(), request.getDisplayName(), role);

            auditService.record("REGISTER", "AUTH", user.getUsername(), "User registered with role: " + role, "SUCCESS", user.getUsername(), user.getId(), servletRequest);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("id", user.getId(), "username", user.getUsername(),
                            "email", user.getEmail(), "role", user.getRole().name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || !jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid refresh token"));
        }

        String username = jwtUtil.getUsernameFromToken(refreshToken);
        AuthUser user = authUserService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "User not found"));
        }

        String newAccessToken = jwtUtil.generateAccessToken(
                username, user.getRole().name());
        String newRefreshToken = jwtUtil.generateRefreshToken(username);

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
                "refreshToken", newRefreshToken,
                "tokenType", "Bearer"));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = jwtUtil.getUsernameFromToken(token);
        AuthUser user = authUserService.findByUsername(username);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        return ResponseEntity.ok(new UserProfile(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getDisplayName(), user.getRole().name(),
                user.isEnabled(), user.getLastLogin(), user.isMfaEnabled()));
    }

    @GetMapping("/users")
    public ResponseEntity<List<AuthUser>> listUsers() {
        return ResponseEntity.ok(authUserService.listAllUsers());
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<AuthUser> updateUser(
            @PathVariable UUID id, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(authUserService.updateUser(
                id, request.getDisplayName(), request.getRole(), request.getEnabled()));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        authUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit")
    public ResponseEntity<List<com.switchplatform.platform.model.auth.AuditLog>> listAudit(@RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(auditService.listAll(limit));
    }

    @GetMapping("/audit/user/{userId}")
    public ResponseEntity<List<com.switchplatform.platform.model.auth.AuditLog>> listAuditByUser(
            @PathVariable UUID userId, @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(auditService.listByUser(userId, limit));
    }

    private String resolveUsername(String provided) {
        if (provided != null) return provided;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return null;
    }

    private UUID resolveUserId(String username) {
        AuthUser user = authUserService.findByUsername(username);
        return user != null ? user.getId() : null;
    }

    @Data
    public static class LoginRequest {
        @NotBlank @Size(min = 3, max = 64)
        private String username;
        @NotBlank @Size(min = 6, max = 128)
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank @Size(min = 3, max = 64)
        private String username;
        @NotBlank @Size(min = 6, max = 128)
        private String password;
        @NotBlank @Email @Size(max = 128)
        private String email;
        private String displayName;
        private String role;
    }

    @Data
    public static class AuthResponse {
        private final String accessToken;
        private final String refreshToken;
        private final String username;
        private final String role;
        private final String displayName;
        private final String email;
        private final String tokenType = "Bearer";
    }

    @Data
    public static class UserProfile {
        private final UUID id;
        private final String username;
        private final String email;
        private final String displayName;
        private final String role;
        private final boolean enabled;
        private final java.time.OffsetDateTime lastLogin;
        private final boolean mfaEnabled;
    }

    @Data
    public static class UpdateUserRequest {
        private String displayName;
        private AuthUser.Role role;
        private Boolean enabled;
    }
}
