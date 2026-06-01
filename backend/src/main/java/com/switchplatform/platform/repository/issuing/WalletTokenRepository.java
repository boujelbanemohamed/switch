package com.switchplatform.platform.repository.issuing;

import com.switchplatform.platform.model.issuing.WalletToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletTokenRepository extends JpaRepository<WalletToken, UUID> {

    Optional<WalletToken> findByToken(String token);

    List<WalletToken> findByCardIdOrderByCreatedAtDesc(UUID cardId);

    List<WalletToken> findByWalletProvider(WalletToken.WalletProvider walletProvider);

    List<WalletToken> findByStatus(WalletToken.TokenStatus status);

    List<WalletToken> findByCardIdAndStatus(UUID cardId, WalletToken.TokenStatus status);

    boolean existsByToken(String token);
}
