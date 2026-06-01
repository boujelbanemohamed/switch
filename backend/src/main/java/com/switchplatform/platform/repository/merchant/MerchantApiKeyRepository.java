package com.switchplatform.platform.repository.merchant;

import com.switchplatform.platform.model.merchant.MerchantApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MerchantApiKeyRepository extends JpaRepository<MerchantApiKey, UUID> {
    List<MerchantApiKey> findByMerchantCode(String merchantCode);
    Optional<MerchantApiKey> findByApiKey(String apiKey);
    Optional<MerchantApiKey> findByMerchantCodeAndEnabledTrue(String merchantCode);
}
