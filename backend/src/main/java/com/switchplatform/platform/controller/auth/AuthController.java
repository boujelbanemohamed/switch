package com.switchplatform.platform.controller.auth;

import com.switchplatform.platform.config.auth.JwtUtil;
import com.switchplatform.platform.model.auth.AuthUser;
import com.switchplatform.platform.service.auth.AuthUserService;
import jakarta.validation.Valid;
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

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(), request.getPassword()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        }

        AuthUser user = authUserService.findByUsername(request.getUsername());
        authUserService.recordLogin(request.getUsername());

        String accessToken = jwtUtil.generateAccessToken(
                user.getUsername(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        return ResponseEntity.ok(new AuthResponse(
                accessToken,
                refreshToken,
                user.getUsername(),
                user.getRole().name(),
                user.getDisplayName(),
                user.getEmail()));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthUser.Role role = request.getRole() != null
                    ? AuthUser.Role.valueOf(request.getRole().toUpperCase())
                    : AuthUser.Role.OPERATOR;

            AuthUser user = authUserService.registerUser(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getDisplayName(),
                    role);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "id", user.getId(),
                            "username", user.getUsername(),
                            "email", user.getEmail(),
                            "role", user.getRole().name()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
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

        return ResponseEntity.ok(Map.of(
                "accessToken", newAccessToken,
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
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getRole().name(),
                user.isEnabled(),
                user.getLastLogin()));
    }

    @GetMapping("/users")
    public ResponseEntity<List<AuthUser>> listUsers() {
        return ResponseEntity.ok(authUserService.listAllUsers());
    }

    @PutMapping("/users/{id}")
    public ResponseEntity<AuthUser> updateUser(
            @PathVariable UUID id,
            @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(authUserService.updateUser(
                id, request.getDisplayName(), request.getRole(), request.getEnabled()));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        authUserService.deleteUser(id);
        return ResponseEntity.noContent().build();
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
    }

    @Data
    public static class UpdateUserRequest {
        private String displayName;
        private AuthUser.Role role;
        private Boolean enabled;
    }
}
