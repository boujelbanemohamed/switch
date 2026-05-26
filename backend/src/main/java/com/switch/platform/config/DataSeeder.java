package com.switch.platform.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.switch.platform.model.Participant;
import com.switch.platform.model.Transaction;
import com.switch.platform.repository.ParticipantRepository;
import com.switch.platform.repository.TransactionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

@Component
@Profile("dev")
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final ParticipantRepository participantRepository;
    private final TransactionRepository transactionRepository;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void seed() {
        if (participantRepository.count() > 0) {
            log.info("Database already seeded, skipping");
            return;
        }

        log.info("Seeding demo data...");

        Participant acquirer = Participant.builder()
                .code("DEMO_ACQ")
                .name("Demo Acquirer Bank")
                .type(Participant.ParticipantType.ACQUIRER)
                .status(Participant.ParticipantStatus.ACTIVE)
                .endpointUrl("demo-acquirer:8001")
                .endpointType(Participant.EndpointType.TCP)
                .supportedProtocols(new String[]{"ISO8583", "ISO20022"})
                .build();

        Participant issuer = Participant.builder()
                .code("DEMO_ISS")
                .name("Demo Issuer Bank")
                .type(Participant.ParticipantType.ISSUER)
                .status(Participant.ParticipantStatus.ACTIVE)
                .endpointUrl("demo-issuer:8002")
                .endpointType(Participant.EndpointType.TCP)
                .supportedProtocols(new String[]{"ISO8583"})
                .build();

        Participant switchMain = Participant.builder()
                .code("SWITCH")
                .name("Main Switch")
                .type(Participant.ParticipantType.SWITCH)
                .status(Participant.ParticipantStatus.ACTIVE)
                .supportedProtocols(new String[]{"ISO8583", "ISO20022"})
                .build();

        acquirer = participantRepository.save(acquirer);
        issuer = participantRepository.save(issuer);
        switchMain = participantRepository.save(switchMain);

        seedDemoTransactions(acquirer, issuer);
    }

    private void seedDemoTransactions(Participant acquirer, Participant issuer) {
        Random random = new Random(42);
        Transaction.TransactionStatus[] statuses = Transaction.TransactionStatus.values();

        for (int i = 0; i < 50; i++) {
            Transaction.TransactionStatus status = statuses[random.nextInt(statuses.length)];
            boolean completed = status == Transaction.TransactionStatus.COMPLETED;
            long amount = Math.abs(random.nextLong() % 10000000);

            Transaction tx = Transaction.builder()
                    .transactionId("DEMO" + String.format("%019d", System.currentTimeMillis() + i))
                    .messageType("0200")
                    .protocol(Transaction.Protocol.ISO8583)
                    .stan(String.format("%06d", random.nextInt(999999)))
                    .rrn(String.format("%012d", random.nextInt(999999999)))
                    .panHash(Base64.getEncoder().encodeToString(
                            ("400000" + String.format("%010d", random.nextInt(999999999))).getBytes()))
                    .amount(BigDecimal.valueOf(amount, 3))
                    .currencyCode(random.nextBoolean() ? "TND" : "EUR")
                    .merchantId("MERCH" + String.format("%05d", random.nextInt(99999)))
                    .terminalId("TERM" + String.format("%04d", random.nextInt(9999)))
                    .acquiringParticipant(acquirer)
                    .issuingParticipant(issuer)
                    .sourceParticipant(acquirer)
                    .destinationParticipant(issuer)
                    .status(status)
                    .responseCode(completed ? "00" : (status == Transaction.TransactionStatus.FAILED ? "05" :
                            status == Transaction.TransactionStatus.REJECTED ? "30" : null))
                    .processingTimeMs(completed ? 50 + random.nextInt(450) : null)
                    .requestAt(OffsetDateTime.now().minusHours(random.nextInt(24)))
                    .responseAt(completed ? OffsetDateTime.now() : null)
                    .build();

            transactionRepository.save(tx);
        }

        log.info("Seeded {} demo transactions", 50);
    }
}
