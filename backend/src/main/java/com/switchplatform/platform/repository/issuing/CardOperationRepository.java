package com.switchplatform.platform.repository.issuing;

import com.switchplatform.platform.model.issuing.CardOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CardOperationRepository extends JpaRepository<CardOperation, Long> {
    List<CardOperation> findByCardIdOrderByCreatedAtDesc(UUID cardId);
}
