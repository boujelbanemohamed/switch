package com.switchplatform.platform.service.admin;

import com.switchplatform.platform.model.admin.LiveConfig;
import com.switchplatform.platform.repository.admin.LiveConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiveConfigServiceTest {

    private LiveConfigService service;
    private LiveConfigRepository repository;
    private final Map<UUID, LiveConfig> store = new ConcurrentHashMap<>();

    @BeforeEach
    void setUp() {
        store.clear();
        repository = mock(LiveConfigRepository.class);

        when(repository.save(any())).thenAnswer(inv -> {
            LiveConfig c = inv.getArgument(0);
            if (c.getId() == null) c.setId(UUID.randomUUID());
            store.put(c.getId(), c);
            return c;
        });
        when(repository.findById(any())).thenAnswer(inv ->
                Optional.ofNullable(store.get(inv.getArgument(0))));
        when(repository.findByConfigKey(any())).thenAnswer(inv -> {
            String key = inv.getArgument(0);
            return store.values().stream().filter(c -> key.equals(c.getConfigKey())).findFirst();
        });
        when(repository.findByCategoryOrderByConfigKey(any())).thenAnswer(inv -> {
            String cat = inv.getArgument(0);
            return store.values().stream()
                    .filter(c -> cat.equals(c.getCategory()))
                    .sorted(Comparator.comparing(LiveConfig::getConfigKey))
                    .toList();
        });
        when(repository.findAllByOrderByCategoryAscConfigKeyAsc()).thenAnswer(inv ->
                store.values().stream()
                        .sorted(Comparator.comparing(LiveConfig::getCategory)
                                .thenComparing(LiveConfig::getConfigKey))
                        .toList());

        service = new LiveConfigService(repository);
    }

    private LiveConfig createConfig(String key, String value, String category, boolean mutable) {
        LiveConfig c = LiveConfig.builder()
                .configKey(key).configValue(value).category(category).mutable(mutable)
                .build();
        return repository.save(c);
    }

    @Test
    void shouldGetAllConfig() {
        createConfig("fee.rate", "1.5", "FEES", true);
        createConfig("txn.limit", "1000", "LIMITS", true);
        assertEquals(2, service.getAllConfig().size());
    }

    @Test
    void shouldGetGroupedByCategory() {
        createConfig("fee.rate", "1.5", "FEES", true);
        createConfig("txn.limit", "1000", "LIMITS", true);
        createConfig("fee.cap", "50", "FEES", true);
        Map<String, List<LiveConfig>> grouped = service.getConfigGroupedByCategory();
        assertEquals(2, grouped.size());
        assertEquals(2, grouped.get("FEES").size());
        assertEquals(1, grouped.get("LIMITS").size());
    }

    @Test
    void shouldGetByKey() {
        createConfig("test.key", "value", "GENERAL", true);
        LiveConfig found = service.getByKey("test.key");
        assertEquals("value", found.getConfigValue());
    }

    @Test
    void shouldThrowWhenKeyNotFound() {
        assertThrows(IllegalArgumentException.class, () -> service.getByKey("unknown"));
    }

    @Test
    void shouldGetByCategory() {
        createConfig("a", "1", "CAT1", true);
        createConfig("b", "2", "CAT1", true);
        createConfig("c", "3", "CAT2", true);
        assertEquals(2, service.getByCategory("CAT1").size());
    }

    @Test
    void shouldUpdateMutableConfig() {
        createConfig("my.key", "old", "GENERAL", true);
        LiveConfig updated = service.updateConfig(
                store.values().iterator().next().getId(), "new", "admin");
        assertEquals("new", updated.getConfigValue());
        assertEquals("admin", updated.getUpdatedBy());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    void shouldThrowWhenUpdatingImmutableConfig() {
        createConfig("my.key", "val", "GENERAL", false);
        UUID id = store.values().iterator().next().getId();
        assertThrows(IllegalStateException.class,
                () -> service.updateConfig(id, "new", "admin"));
    }

    @Test
    void shouldThrowWhenConfigNotFound() {
        assertThrows(IllegalArgumentException.class,
                () -> service.updateConfig(UUID.randomUUID(), "val", "admin"));
    }

    @Test
    void shouldGetValue() {
        createConfig("str.key", "hello", "GENERAL", true);
        assertEquals("hello", service.getValue("str.key"));
        assertNull(service.getValue("nonexistent"));
    }

    @Test
    void shouldGetBoolean() {
        createConfig("flag", "true", "GENERAL", true);
        assertTrue(service.getBoolean("flag", false));
        assertFalse(service.getBoolean("nonexistent", false));
    }

    @Test
    void shouldGetInt() {
        createConfig("num", "42", "GENERAL", true);
        assertEquals(42, service.getInt("num", 0));
        assertEquals(10, service.getInt("nonexistent", 10));
    }

    @Test
    void shouldGetLong() {
        createConfig("big", "9999999999", "GENERAL", true);
        assertEquals(9999999999L, service.getLong("big", 0));
    }

    @Test
    void shouldGetDouble() {
        createConfig("pi", "3.14159", "GENERAL", true);
        assertEquals(3.14159, service.getDouble("pi", 0.0), 0.00001);
    }
}
