package com.switchplatform.platform.repository.issuing;

import com.switchplatform.platform.model.issuing.CardProduct;
import com.switchplatform.platform.model.issuing.CardProduct.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardProductRepository extends JpaRepository<CardProduct, UUID> {

    List<CardProduct> findByProgramId(UUID programId);

    List<CardProduct> findByCardBrand(CardProduct.CardBrand cardBrand);

    List<CardProduct> findByCardType(CardProduct.CardType cardType);

    List<CardProduct> findByStatus(Status status);

    Optional<CardProduct> findByProductCode(String productCode);

    List<CardProduct> findByProgramIdAndStatus(UUID programId, Status status);
}
