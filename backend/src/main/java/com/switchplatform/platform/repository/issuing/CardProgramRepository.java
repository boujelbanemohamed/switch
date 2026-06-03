package com.switchplatform.platform.repository.issuing;

import com.switchplatform.platform.model.issuing.CardProgram;
import com.switchplatform.platform.model.issuing.CardProgram.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardProgramRepository extends JpaRepository<CardProgram, UUID> {

    List<CardProgram> findByStatus(Status status);

    List<CardProgram> findByProgramType(CardProgram.ProgramType programType);

    List<CardProgram> findByBrand(String brand);
}
