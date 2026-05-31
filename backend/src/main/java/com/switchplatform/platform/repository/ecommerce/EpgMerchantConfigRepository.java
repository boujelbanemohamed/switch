package com.switchplatform.platform.repository.ecommerce;

import com.switchplatform.platform.model.ecommerce.EpgMerchantConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EpgMerchantConfigRepository extends JpaRepository<EpgMerchantConfig, UUID> {

    Optional<EpgMerchantConfig> findByMerchantId(UUID merchantId);

    @Query("SELECT e FROM EpgMerchantConfig e WHERE e.apiKeyHash = :apiKey")
    Optional<EpgMerchantConfig> findByApiKey(@Param("apiKey") String apiKey);
}
