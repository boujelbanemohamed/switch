package com.switchplatform.platform.service.issuing;

import com.switchplatform.platform.model.issuing.CardAccount;
import com.switchplatform.platform.repository.issuing.CardAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class CardAccountService {

    private final CardAccountRepository cardAccountRepository;

    @Transactional
    public CardAccount createAccount(CardAccount account) {
        if (account.getId() == null) {
            account.setId(UUID.randomUUID());
        }
        if (account.getBalance() == null) {
            account.setBalance(BigDecimal.ZERO);
        }
        if (account.getAvailableBalance() == null) {
            account.setAvailableBalance(BigDecimal.ZERO);
        }
        if (account.getHoldAmount() == null) {
            account.setHoldAmount(BigDecimal.ZERO);
        }
        if (account.getStatus() == null) {
            account.setStatus(CardAccount.AccountStatus.ACTIVE);
        }
        if (account.getCreatedAt() == null) {
            account.setCreatedAt(OffsetDateTime.now());
        }
        if (account.getUpdatedAt() == null) {
            account.setUpdatedAt(OffsetDateTime.now());
        }
        cardAccountRepository.save(account);
        log.info("CardAccount created: id={}, cardholderId={}, balance={}",
                account.getId(), account.getCardholderId(), account.getBalance());
        return account;
    }

    public Optional<CardAccount> getAccount(UUID id) {
        return cardAccountRepository.findById(id);
    }

    public List<CardAccount> getAccountsByCardholderId(UUID cardholderId) {
        return cardAccountRepository.findByCardholderId(cardholderId);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CardAccount debit(UUID accountId, BigDecimal amount, String currencyCode) {
        CardAccount account = cardAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient available balance");
        }
        account.setBalance(account.getBalance().subtract(amount));
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setUpdatedAt(OffsetDateTime.now());
        log.info("Account debited: id={}, amount={}, newBalance={}", accountId, amount, account.getBalance());
        return account;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CardAccount credit(UUID accountId, BigDecimal amount, String currencyCode) {
        CardAccount account = cardAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        account.setBalance(account.getBalance().add(amount));
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        account.setUpdatedAt(OffsetDateTime.now());
        log.info("Account credited: id={}, amount={}, newBalance={}", accountId, amount, account.getBalance());
        return account;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CardAccount hold(UUID accountId, BigDecimal amount) {
        CardAccount account = cardAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Insufficient available balance for hold");
        }
        account.setHoldAmount(account.getHoldAmount().add(amount));
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        account.setUpdatedAt(OffsetDateTime.now());
        log.info("Hold placed: accountId={}, amount={}, hold={}, available={}",
                accountId, amount, account.getHoldAmount(), account.getAvailableBalance());
        return account;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CardAccount releaseHold(UUID accountId, BigDecimal amount) {
        CardAccount account = cardAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));
        BigDecimal newHold = account.getHoldAmount().subtract(amount);
        if (newHold.compareTo(BigDecimal.ZERO) < 0) {
            newHold = BigDecimal.ZERO;
        }
        account.setHoldAmount(newHold);
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        account.setUpdatedAt(OffsetDateTime.now());
        log.info("Hold released: accountId={}, amount={}, available={}",
                accountId, amount, account.getAvailableBalance());
        return account;
    }

    public List<CardAccount> listAll() {
        return cardAccountRepository.findAll();
    }
}
