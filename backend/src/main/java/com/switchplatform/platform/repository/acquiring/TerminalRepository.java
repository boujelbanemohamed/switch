package com.switchplatform.platform.repository.acquiring;

import com.switchplatform.platform.model.acquiring.Terminal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TerminalRepository extends JpaRepository<Terminal, UUID> {
    Optional<Terminal> findByTerminalId(String terminalId);
    List<Terminal> findByMerchantId(UUID merchantId);
    boolean existsByTerminalId(String terminalId);
}
