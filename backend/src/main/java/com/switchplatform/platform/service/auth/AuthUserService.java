package com.switchplatform.platform.service.auth;

import com.switchplatform.platform.model.auth.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthUserService implements UserDetailsService {

    private final Map<UUID, AuthUser> users = new ConcurrentHashMap<>();
    private final Map<String, UUID> usernameIndex = new ConcurrentHashMap<>();
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void seedDefaultUsers() {
        if (!users.isEmpty()) return;
        log.info("Seeding default auth users");
        registerUser("admin", "admin123", "admin@switch.local", "Administrator", AuthUser.Role.ADMIN);
        registerUser("operator", "operator123", "operator@switch.local", "Operator", AuthUser.Role.OPERATOR);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AuthUser authUser = findByUsername(username);
        if (authUser == null) {
            throw new UsernameNotFoundException("User not found: " + username);
        }
        return new User(
                authUser.getUsername(),
                authUser.getPassword(),
                authUser.isEnabled(),
                authUser.isAccountNonExpired(),
                true,
                authUser.isAccountNonLocked(),
                List.of(new SimpleGrantedAuthority("ROLE_" + authUser.getRole().name())));
    }

    public AuthUser registerUser(String username, String password, String email,
                                  String displayName, AuthUser.Role role) {
        if (findByUsername(username) != null) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }
        if (findByEmail(email) != null) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        AuthUser user = AuthUser.builder()
                .id(UUID.randomUUID())
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .displayName(displayName != null ? displayName : username)
                .role(role != null ? role : AuthUser.Role.OPERATOR)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        users.put(user.getId(), user);
        usernameIndex.put(username.toLowerCase(), user.getId());
        log.info("User registered: username={}, role={}", username, role);
        return user;
    }

    public Optional<AuthUser> findById(UUID id) {
        return Optional.ofNullable(users.get(id));
    }

    public AuthUser findByUsername(String username) {
        UUID id = usernameIndex.get(username.toLowerCase());
        return id != null ? users.get(id) : null;
    }

    public AuthUser findByEmail(String email) {
        return users.values().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

    public List<AuthUser> listAllUsers() {
        return users.values().stream()
                .sorted(Comparator.comparing(AuthUser::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public AuthUser updateUser(UUID id, String displayName, AuthUser.Role role, Boolean enabled) {
        AuthUser user = users.get(id);
        if (user == null) {
            throw new IllegalArgumentException("User not found: " + id);
        }
        if (displayName != null) user.setDisplayName(displayName);
        if (role != null) user.setRole(role);
        if (enabled != null) user.setEnabled(enabled);
        user.setUpdatedAt(OffsetDateTime.now());
        log.info("User updated: id={}, role={}, enabled={}", id, role, enabled);
        return user;
    }

    public void recordLogin(String username) {
        AuthUser user = findByUsername(username);
        if (user != null) {
            user.setLastLogin(OffsetDateTime.now());
        }
    }

    public void deleteUser(UUID id) {
        AuthUser user = users.remove(id);
        if (user != null) {
            usernameIndex.remove(user.getUsername().toLowerCase());
            log.info("User deleted: id={}", id);
        }
    }
}
