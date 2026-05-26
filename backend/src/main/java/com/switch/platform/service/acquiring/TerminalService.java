package com.switch.platform.service.acquiring;

import com.switch.platform.model.acquiring.Terminal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TerminalService {

    private final ConcurrentMap<UUID, Terminal> terminalStore = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, UUID> terminalIdIndex = new ConcurrentHashMap<>();

    @Transactional
    public Terminal registerTerminal(Terminal terminal) {
        if (terminal.getTerminalId() == null || terminal.getTerminalId().isBlank()) {
            throw new IllegalArgumentException("terminalId is required");
        }
        if (terminalIdIndex.containsKey(terminal.getTerminalId())) {
            throw new IllegalArgumentException("terminalId already exists: " + terminal.getTerminalId());
        }
        if (terminal.getId() == null) {
            terminal.setId(UUID.randomUUID());
        }
        terminal.setStatus(Terminal.TerminalStatus.ACTIVE);
        terminal.setCreatedAt(OffsetDateTime.now());
        terminal.setUpdatedAt(OffsetDateTime.now());

        terminalStore.put(terminal.getId(), terminal);
        terminalIdIndex.put(terminal.getTerminalId(), terminal.getId());
        log.info("Registered terminal {} (TID: {}) for merchant {}", terminal.getId(), terminal.getTerminalId(), terminal.getMerchantId());
        return terminal;
    }

    @Transactional(readOnly = true)
    public Optional<Terminal> getTerminal(UUID id) {
        return Optional.ofNullable(terminalStore.get(id));
    }

    @Transactional(readOnly = true)
    public Optional<Terminal> getTerminalByTid(String terminalId) {
        return Optional.ofNullable(terminalIdIndex.get(terminalId))
                .map(terminalStore::get);
    }

    @Transactional(readOnly = true)
    public List<Terminal> listByMerchant(UUID merchantId) {
        return terminalStore.values().stream()
                .filter(t -> t.getMerchantId().equals(merchantId))
                .collect(Collectors.toList());
    }

    @Transactional
    public Terminal updateStatus(UUID id, String status) {
        Terminal terminal = getTerminalOrThrow(id);
        terminal.setStatus(Terminal.TerminalStatus.valueOf(status));
        terminal.setUpdatedAt(OffsetDateTime.now());
        log.info("Updated terminal {} status to {}", id, status);
        return terminal;
    }

    @Transactional
    public Terminal updateFirmware(UUID id, String version) {
        Terminal terminal = getTerminalOrThrow(id);
        terminal.setFirmwareVersion(version);
        terminal.setLastContact(OffsetDateTime.now());
        terminal.setUpdatedAt(OffsetDateTime.now());
        log.info("Updated terminal {} firmware to {}", id, version);
        return terminal;
    }

    private Terminal getTerminalOrThrow(UUID id) {
        Terminal terminal = terminalStore.get(id);
        if (terminal == null) {
            throw new IllegalArgumentException("Terminal not found: " + id);
        }
        return terminal;
    }
}
