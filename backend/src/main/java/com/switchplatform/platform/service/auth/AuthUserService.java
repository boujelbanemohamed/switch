package com.switchplatform.platform.service.auth;

import com.switchplatform.platform.model.auth.AuthUser;
import com.switchplatform.platform.repository.auth.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthUserService implements UserDetailsService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final AuthUserRepository authUserRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void seedDefaultUsers() {
        if (authUserRepository.count() > 0) return;
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
        if (authUser.getLockedUntil() != null && authUser.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new UsernameNotFoundException("Account locked until " + authUser.getLockedUntil());
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

    @Transactional(readOnly = true)
    public boolean isAccountLocked(String username) {
        AuthUser user = findByUsername(username);
        if (user == null) return false;
        if (user.getLockedUntil() == null) return false;
        if (user.getLockedUntil().isBefore(OffsetDateTime.now())) {
            user.setLockedUntil(null);
            user.setFailedAttempts(0);
            authUserRepository.save(user);
            return false;
        }
        return true;
    }

    @Transactional
    public void recordFailedAttempt(String username) {
        AuthUser user = findByUsername(username);
        if (user == null) return;
        user.setFailedAttempts(user.getFailedAttempts() + 1);
        if (user.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(OffsetDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
            log.warn("Account locked after {} failed attempts: {}", MAX_FAILED_ATTEMPTS, username);
        }
        user.setUpdatedAt(OffsetDateTime.now());
        authUserRepository.save(user);
    }

    @Transactional
    public void resetFailedAttempts(String username) {
        AuthUser user = findByUsername(username);
        if (user == null) return;
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setUpdatedAt(OffsetDateTime.now());
        authUserRepository.save(user);
    }

    @Transactional
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
                .mfaEnabled(false)
                .failedAttempts(0)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        user = authUserRepository.save(user);
        log.info("User registered: username={}, role={}", username, role);
        return user;
    }

    @Transactional(readOnly = true)
    public Optional<AuthUser> findById(UUID id) {
        return authUserRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public AuthUser findByUsername(String username) {
        return authUserRepository.findByUsernameIgnoreCase(username).orElse(null);
    }

    @Transactional(readOnly = true)
    public AuthUser findByEmail(String email) {
        return authUserRepository.findByEmailIgnoreCase(email).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<AuthUser> listAllUsers() {
        return authUserRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public Page<AuthUser> listAllUsers(int page, int size) {
        return authUserRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Transactional
    public AuthUser updateUser(UUID id, String displayName, AuthUser.Role role, Boolean enabled) {
        AuthUser user = authUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + id));
        if (displayName != null) user.setDisplayName(displayName);
        if (role != null) user.setRole(role);
        if (enabled != null) user.setEnabled(enabled);
        user.setUpdatedAt(OffsetDateTime.now());
        authUserRepository.save(user);
        log.info("User updated: id={}, role={}, enabled={}", id, role, enabled);
        return user;
    }

    @Transactional
    public void recordLogin(String username) {
        AuthUser user = findByUsername(username);
        if (user != null) {
            user.setLastLogin(OffsetDateTime.now());
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            user.setUpdatedAt(OffsetDateTime.now());
            authUserRepository.save(user);
        }
    }

    @Transactional
    public void deleteUser(UUID id) {
        authUserRepository.deleteById(id);
        log.info("User deleted: id={}", id);
    }
}
