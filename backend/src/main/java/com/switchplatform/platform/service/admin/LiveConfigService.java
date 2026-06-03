package com.switchplatform.platform.service.admin;

import com.switchplatform.platform.model.admin.LiveConfig;
import com.switchplatform.platform.repository.admin.LiveConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveConfigService {

    private final LiveConfigRepository repository;

    public List<LiveConfig> getAllConfig() {
        return repository.findAllByOrderByCategoryAscConfigKeyAsc();
    }

    public Map<String, List<LiveConfig>> getConfigGroupedByCategory() {
        Map<String, List<LiveConfig>> grouped = new LinkedHashMap<>();
        for (LiveConfig cfg : getAllConfig()) {
            grouped.computeIfAbsent(cfg.getCategory(), k -> new ArrayList<>()).add(cfg);
        }
        return grouped;
    }

    public LiveConfig getByKey(String key) {
        return repository.findByConfigKey(key)
                .orElseThrow(() -> new IllegalArgumentException("Config not found: " + key));
    }

    public List<LiveConfig> getByCategory(String category) {
        return repository.findByCategoryOrderByConfigKey(category);
    }

    public LiveConfig updateConfig(UUID id, String newValue, String updatedBy) {
        LiveConfig cfg = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Config not found: " + id));
        if (!cfg.getMutable()) {
            throw new IllegalStateException("Config " + cfg.getConfigKey() + " is immutable");
        }
        cfg.setConfigValue(newValue);
        cfg.setUpdatedBy(updatedBy);
        cfg.setUpdatedAt(OffsetDateTime.now());
        LiveConfig saved = repository.save(cfg);
        log.info("Live config updated: {} = {} (by {})", cfg.getConfigKey(), newValue, updatedBy);
        return saved;
    }

    public String getValue(String key) {
        return repository.findByConfigKey(key)
                .map(LiveConfig::getConfigValue)
                .orElse(null);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String val = getValue(key);
        return val != null ? Boolean.parseBoolean(val) : defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        String val = getValue(key);
        if (val == null) return defaultValue;
        try { return Integer.parseInt(val); } catch (NumberFormatException e) { return defaultValue; }
    }

    public long getLong(String key, long defaultValue) {
        String val = getValue(key);
        if (val == null) return defaultValue;
        try { return Long.parseLong(val); } catch (NumberFormatException e) { return defaultValue; }
    }

    public double getDouble(String key, double defaultValue) {
        String val = getValue(key);
        if (val == null) return defaultValue;
        try { return Double.parseDouble(val); } catch (NumberFormatException e) { return defaultValue; }
    }
}
