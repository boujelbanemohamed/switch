package com.switchplatform.platform.repository.admin;

import com.switchplatform.platform.model.admin.LiveConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LiveConfigRepository extends JpaRepository<LiveConfig, UUID> {
    Optional<LiveConfig> findByConfigKey(String configKey);
    List<LiveConfig> findByCategoryOrderByConfigKey(String category);
    List<LiveConfig> findAllByOrderByCategoryAscConfigKeyAsc();
}
