package com.switchplatform.platform.repository.loyalty;

import com.switchplatform.platform.model.loyalty.LoyaltyProgram;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface LoyaltyProgramRepository extends JpaRepository<LoyaltyProgram, UUID> {
    List<LoyaltyProgram> findByStatus(LoyaltyProgram.ProgramStatus status);
}
