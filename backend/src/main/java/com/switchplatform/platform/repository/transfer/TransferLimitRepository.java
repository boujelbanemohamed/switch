package com.switchplatform.platform.repository.transfer;

import com.switchplatform.platform.model.transfer.TransferLimit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransferLimitRepository extends JpaRepository<TransferLimit, UUID> {
    Optional<TransferLimit> findByTransferTypeAndStatus(String transferType, String status);
}
