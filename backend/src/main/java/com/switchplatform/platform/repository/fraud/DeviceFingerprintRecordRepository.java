package com.switchplatform.platform.repository.fraud;

import com.switchplatform.platform.model.fraud.DeviceFingerprintRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceFingerprintRecordRepository extends JpaRepository<DeviceFingerprintRecord, UUID> {
    List<DeviceFingerprintRecord> findByCardIdOrderByLastSeenDesc(String cardId);
    Optional<DeviceFingerprintRecord> findByCardIdAndDeviceId(String cardId, String deviceId);
    boolean existsByCardIdAndDeviceId(String cardId, String deviceId);
    List<DeviceFingerprintRecord> findByDeviceId(String deviceId);
}
