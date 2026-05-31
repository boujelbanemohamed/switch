package com.switchplatform.platform.repository.clearing;

import com.switchplatform.platform.model.clearing.InterchangeFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InterchangeFeeRepository extends JpaRepository<InterchangeFee, UUID> {
    Optional<InterchangeFee> findByBrandAndCardTypeAndRegionAndMcc(String brand, String cardType, String region, String mcc);
    List<InterchangeFee> findByBrand(String brand);
}
