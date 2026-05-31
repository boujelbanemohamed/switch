package com.switchplatform.platform.service.acquiring;

import com.switchplatform.platform.model.acquiring.Terminal;
import com.switchplatform.platform.repository.acquiring.TerminalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerminalService {

    private final TerminalRepository terminalRepository;

    @Transactional
    public Terminal registerTerminal(Terminal terminal) {
        if (terminal.getTerminalId() == null || terminal.getTerminalId().isBlank()) {
            throw new IllegalArgumentException("terminalId is required");
        }
        if (terminalRepository.existsByTerminalId(terminal.getTerminalId())) {
            throw new IllegalArgumentException("terminalId already exists: " + terminal.getTerminalId());
        }
        if (terminal.getId() == null) {
            terminal.setId(UUID.randomUUID());
        }
        terminal.setStatus(Terminal.TerminalStatus.ACTIVE);
        terminal.setCreatedAt(OffsetDateTime.now());
        terminal.setUpdatedAt(OffsetDateTime.now());

        terminalRepository.save(terminal);
        log.info("Registered terminal {} (TID: {}) for merchant {}", terminal.getId(), terminal.getTerminalId(), terminal.getMerchantId());
        return terminal;
    }

    @Transactional(readOnly = true)
    public Optional<Terminal> getTerminal(UUID id) {
        return terminalRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Terminal> getTerminalByTid(String terminalId) {
        return terminalRepository.findByTerminalId(terminalId);
    }

    @Transactional(readOnly = true)
    public List<Terminal> listByMerchant(UUID merchantId) {
        return terminalRepository.findByMerchantId(merchantId);
    }

    @Transactional
    public Terminal updateStatus(UUID id, String status) {
        Terminal terminal = getTerminalOrThrow(id);
        terminal.setStatus(Terminal.TerminalStatus.valueOf(status));
        terminal.setUpdatedAt(OffsetDateTime.now());
        log.info("Updated terminal {} status to {}", id, status);
        return terminalRepository.save(terminal);
    }

    @Transactional
    public Terminal updateFirmware(UUID id, String version) {
        Terminal terminal = getTerminalOrThrow(id);
        terminal.setFirmwareVersion(version);
        terminal.setLastContact(OffsetDateTime.now());
        terminal.setUpdatedAt(OffsetDateTime.now());
        log.info("Updated terminal {} firmware to {}", id, version);
        return terminalRepository.save(terminal);
    }

    private Terminal getTerminalOrThrow(UUID id) {
        return terminalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Terminal not found: " + id));
    }
}
