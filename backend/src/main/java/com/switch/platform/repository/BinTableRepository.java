package com.switch.platform.repository;

import com.switch.platform.model.BinTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BinTableRepository extends JpaRepository<BinTable, UUID> {
    Optional<BinTable> findByBinAndIsActiveTrue(String bin);
    List<BinTable> findByParticipantId(UUID participantId);
    List<BinTable> findByCardBrand(String cardBrand);
}
