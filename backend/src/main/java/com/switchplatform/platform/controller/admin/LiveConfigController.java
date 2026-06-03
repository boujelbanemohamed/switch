package com.switchplatform.platform.controller.admin;

import com.switchplatform.platform.model.admin.LiveConfig;
import com.switchplatform.platform.service.admin.LiveConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/live-config")
@RequiredArgsConstructor
public class LiveConfigController {

    private final LiveConfigService liveConfigService;

    @GetMapping
    public ResponseEntity<List<LiveConfig>> getAll() {
        return ResponseEntity.ok(liveConfigService.getAllConfig());
    }

    @GetMapping("/grouped")
    public ResponseEntity<Map<String, List<LiveConfig>>> getGrouped() {
        return ResponseEntity.ok(liveConfigService.getConfigGroupedByCategory());
    }

    @GetMapping("/{key}")
    public ResponseEntity<LiveConfig> getByKey(@PathVariable String key) {
        return ResponseEntity.ok(liveConfigService.getByKey(key));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<List<LiveConfig>> getByCategory(@PathVariable String category) {
        return ResponseEntity.ok(liveConfigService.getByCategory(category));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LiveConfig> update(@PathVariable UUID id,
                                              @RequestBody Map<String, String> body,
                                              Authentication auth) {
        String updatedBy = auth != null ? auth.getName() : "system";
        return ResponseEntity.ok(liveConfigService.updateConfig(id, body.get("value"), updatedBy));
    }
}
