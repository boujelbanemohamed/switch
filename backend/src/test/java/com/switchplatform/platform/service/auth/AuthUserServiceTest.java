package com.switchplatform.platform.service.auth;

import com.switchplatform.platform.model.auth.AuthUser;
import com.switchplatform.platform.repository.auth.AuthUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthUserServiceTest {

    private AuthUserService service;
    private AuthUserRepository repository;
    private PasswordEncoder passwordEncoder;
    private final Map<UUID, AuthUser> store = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        store.clear();
        repository = mock(AuthUserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);

        when(passwordEncoder.encode(any())).thenAnswer(inv -> "encoded:" + inv.getArgument(0));
        when(passwordEncoder.matches(any(), any())).thenAnswer(inv -> {
            String raw = inv.getArgument(0);
            String encoded = inv.getArgument(1);
            return encoded.equals("encoded:" + raw);
        });

        when(repository.save(any())).thenAnswer(inv -> {
            AuthUser u = inv.getArgument(0);
            if (u.getId() == null) u.setId(UUID.randomUUID());
            store.put(u.getId(), u);
            return u;
        });
        when(repository.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(store.get(inv.getArgument(0))));
        when(repository.findByUsernameIgnoreCase(any())).thenAnswer(inv -> {
            String username = inv.getArgument(0);
            return store.values().stream().filter(u -> username.equals(u.getUsername())).findFirst();
        });
        when(repository.findByEmailIgnoreCase(any())).thenAnswer(inv -> {
            String email = inv.getArgument(0);
            return store.values().stream().filter(u -> email.equals(u.getEmail())).findFirst();
        });
        when(repository.existsByUsernameIgnoreCase(any())).thenAnswer(inv -> {
            String username = inv.getArgument(0);
            return store.values().stream().anyMatch(u -> username.equals(u.getUsername()));
        });
        when(repository.existsByEmailIgnoreCase(any())).thenAnswer(inv -> {
            String email = inv.getArgument(0);
            return store.values().stream().anyMatch(u -> email.equals(u.getEmail()));
        });
        when(repository.findAll(any(Sort.class))).thenAnswer(inv -> {
            List<AuthUser> all = new ArrayList<>(store.values());
            all.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            return all;
        });
        when(repository.findAll(any(Pageable.class))).thenAnswer(inv -> {
            Pageable p = inv.getArgument(0);
            List<AuthUser> all = new ArrayList<>(store.values());
            all.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            int start = (int) p.getOffset();
            int end = Math.min(start + p.getPageSize(), all.size());
            List<AuthUser> content = start < all.size() ? all.subList(start, end) : Collections.emptyList();
            return new org.springframework.data.domain.PageImpl<>(content, p, all.size());
        });
        doAnswer(inv -> {
            store.remove(inv.getArgument(0));
            return null;
        }).when(repository).deleteById(any());

        service = new AuthUserService(repository, passwordEncoder);
    }

    @Test
    void shouldRegisterUser() {
        AuthUser user = service.registerUser("newuser", "pass123", "new@test.com",
                "New User", AuthUser.Role.OPERATOR);
        assertNotNull(user.getId());
        assertTrue(user.getPassword().startsWith("encoded:"));
        assertEquals("newuser", user.getUsername());
        assertEquals(AuthUser.Role.OPERATOR, user.getRole());
    }

    @Test
    void shouldThrowOnDuplicateUsername() {
        service.registerUser("dup", "pass", "a@test.com", "A", AuthUser.Role.OPERATOR);
        assertThrows(IllegalArgumentException.class,
                () -> service.registerUser("dup", "pass", "b@test.com", "B", AuthUser.Role.OPERATOR));
    }

    @Test
    void shouldThrowOnDuplicateEmail() {
        service.registerUser("a", "pass", "same@test.com", "A", AuthUser.Role.OPERATOR);
        assertThrows(IllegalArgumentException.class,
                () -> service.registerUser("b", "pass", "same@test.com", "B", AuthUser.Role.OPERATOR));
    }

    @Test
    void shouldLoadUserByUsername() {
        service.registerUser("loadme", "pass", "load@test.com", "Load", AuthUser.Role.OPERATOR);
        UserDetails ud = service.loadUserByUsername("loadme");
        assertNotNull(ud);
        assertEquals("loadme", ud.getUsername());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        assertThrows(UsernameNotFoundException.class,
                () -> service.loadUserByUsername("nonexistent"));
    }

    @Test
    void shouldLoadUserDetails() {
        service.registerUser("authme", "correct", "auth@test.com", "Auth", AuthUser.Role.OPERATOR);
        UserDetails ud = service.loadUserByUsername("authme");
        assertNotNull(ud);
        assertEquals("authme", ud.getUsername());
    }

    @Test
    void shouldNotLoadWhenLocked() {
        AuthUser u = service.registerUser("locked", "pass", "locked@test.com", "Locked", AuthUser.Role.OPERATOR);
        u.setLockedUntil(OffsetDateTime.now().plusHours(1));
        repository.save(u);
        assertTrue(service.isAccountLocked("locked"));
        assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("locked"));
    }

    @Test
    void shouldTrackFailedAttempts() {
        service.registerUser("failme", "pass", "fail@test.com", "Fail", AuthUser.Role.OPERATOR);
        for (int i = 0; i < 5; i++) {
            service.recordFailedAttempt("failme");
        }
        AuthUser u = repository.findByUsernameIgnoreCase("failme").orElseThrow();
        assertEquals(5, u.getFailedAttempts());
    }

    @Test
    void shouldLockAfterMaxAttempts() {
        service.registerUser("lockme", "pass", "lock@test.com", "Lock", AuthUser.Role.OPERATOR);
        for (int i = 0; i < 6; i++) {
            service.recordFailedAttempt("lockme");
        }
        AuthUser u = repository.findByUsernameIgnoreCase("lockme").orElseThrow();
        assertTrue(u.getFailedAttempts() >= 5);
    }

    @Test
    void shouldResetFailedAttemptsOnSuccessfulLogin() {
        service.registerUser("reset", "pass", "reset@test.com", "Reset", AuthUser.Role.OPERATOR);
        service.recordFailedAttempt("reset");
        service.recordFailedAttempt("reset");
        service.recordLogin("reset");
        AuthUser u = repository.findByUsernameIgnoreCase("reset").orElseThrow();
        assertEquals(0, u.getFailedAttempts());
    }

    @Test
    void shouldUpdateUser() {
        service.registerUser("update", "pass", "update@test.com", "Update", AuthUser.Role.OPERATOR);
        AuthUser u = repository.findByUsernameIgnoreCase("update").orElseThrow();
        AuthUser updated = service.updateUser(u.getId(), "Updated Name", AuthUser.Role.ADMIN, true);
        assertEquals("Updated Name", updated.getDisplayName());
        assertEquals(AuthUser.Role.ADMIN, updated.getRole());
    }

    @Test
    void shouldListAllUsers() {
        service.registerUser("u1", "pass", "u1@test.com", "U1", AuthUser.Role.OPERATOR);
        service.registerUser("u2", "pass", "u2@test.com", "U2", AuthUser.Role.ADMIN);
        assertEquals(2, service.listAllUsers().size());
    }

    @Test
    void shouldListAllUsersPaginated() {
        for (int i = 0; i < 6; i++) {
            service.registerUser("user" + i, "pass", "u" + i + "@test.com", "U" + i, AuthUser.Role.OPERATOR);
        }
        Page<AuthUser> page = service.listAllUsers(0, 3);
        assertEquals(3, page.getContent().size());
        assertEquals(6, page.getTotalElements());
    }

    @Test
    void shouldDeleteUser() {
        AuthUser u = service.registerUser("delete", "pass", "del@test.com", "Del", AuthUser.Role.OPERATOR);
        service.deleteUser(u.getId());
        assertThrows(IllegalArgumentException.class, () -> service.updateUser(u.getId(), null, null, null));
    }

    @Test
    void shouldEnableAndDisableUser() {
        AuthUser u = service.registerUser("toggle", "pass", "toggle@test.com", "Toggle", AuthUser.Role.OPERATOR);
        assertTrue(u.isEnabled());
        service.updateUser(u.getId(), null, null, false);
        AuthUser disabled = repository.findByUsernameIgnoreCase("toggle").orElseThrow();
        assertFalse(disabled.isEnabled());
    }
}
