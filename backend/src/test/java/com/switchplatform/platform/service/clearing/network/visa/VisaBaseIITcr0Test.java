package com.switchplatform.platform.service.clearing.network.visa;

import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.acquiring.Merchant;
import com.switchplatform.platform.model.clearing.ClearingRecord;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.repository.acquiring.MerchantRepository;
import com.switchplatform.platform.repository.clearing.ClearingRecordRepository;
import com.switchplatform.platform.service.clearing.network.VisaBaseIIGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VisaBaseIITcr0Test {

    private VisaBaseIIGenerator generator;
    private ParticipantRepository participantRepository;
    private MerchantRepository merchantRepository;
    private ClearingRecordRepository clearingRecordRepository;

    private final UUID ACQ_ID = UUID.randomUUID();
    private final UUID ISS_ID = UUID.randomUUID();
    private final LocalDate CLEARING_DATE = LocalDate.of(2026, 6, 5);
    private ClearingRecord sampleRecord;

    @BeforeEach
    void setUp() {
        participantRepository = mock(ParticipantRepository.class);
        merchantRepository = mock(MerchantRepository.class);
        clearingRecordRepository = mock(ClearingRecordRepository.class);
        generator = new VisaBaseIIGenerator(participantRepository, merchantRepository, clearingRecordRepository);

        Participant acquirer = Participant.builder()
                .id(ACQ_ID)
                .code("BNA")
                .bankCode("BNA01")
                .name("Banque Nationale Agricole")
                .build();
        when(participantRepository.findById(ACQ_ID)).thenReturn(Optional.of(acquirer));

        Merchant merchant = Merchant.builder()
                .merchantId("MCH0000123")
                .tradingName("SUPERMARCHE CARREFOUR")
                .city("TUNIS")
                .postalCode("1002")
                .countryCode("788")
                .build();
        when(merchantRepository.findByMerchantId("MCH0000123")).thenReturn(Optional.of(merchant));

        sampleRecord = ClearingRecord.builder()
                .id(UUID.randomUUID())
                .transactionId("TXN-TEST-001")
                .clearingDate(CLEARING_DATE)
                .transactionDate(OffsetDateTime.of(2026, 6, 5, 10, 30, 0, 0, OffsetDateTime.now().getOffset()))
                .acquiringParticipantId(ACQ_ID)
                .issuingParticipantId(ISS_ID)
                .cardNumber("1234567890123456")
                .amount(new BigDecimal("150.000"))
                .currencyCode("TND")
                .mcc("5411")
                .tradingName("SUPERMARCHE CARREFOUR")
                .merchantNumber("MCH0000123")
                .authorizationNumber("AUTH12")
                .archiveReference("ARN20260605TXN00123")
                .build();
    }

    @Test
    void tcr0_exactLength_168() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        assertEquals(168, line.length(), "TC05 TCR0 must be exactly 168 characters");
    }

    @Test
    void tcr0_transactionCode_is05() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        assertEquals("05", line.substring(0, 2));
    }

    @Test
    void tcr0_pan_position5to20() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        String panField = line.substring(4, 20);
        // PAN is 16 chars, an-formatted (left-justified, space-padded)
        assertEquals(16, panField.length(), "PAN field should be 16 chars");
        assertTrue(panField.startsWith("1234567890123456"), "PAN should contain card number");
    }

    @Test
    void tcr0_panExtension_is000() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        assertEquals("000", line.substring(20, 23));
    }

    @Test
    void tcr0_arn_position27to49() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        String arnField = line.substring(26, 49);
        assertEquals(23, arnField.length(), "ARN field should be 23 chars");
        assertTrue(arnField.startsWith("ARN20260605TXN00123"), "ARN should match archiveReference");
    }

    @Test
    void tcr0_acquirerBid_position50to57() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        String bidField = line.substring(49, 57);
        assertEquals(8, bidField.length(), "Acquirer BID field should be 8 chars");
        assertTrue(bidField.startsWith("BNA01"), "BID should contain bank code");
    }

    @Test
    void tcr0_purchaseDate_position58to61() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        assertEquals("0605", line.substring(57, 61), "Purchase date should be MMDD");
    }

    @Test
    void tcr0_destAmount_position62to73() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        String amountField = line.substring(61, 73);
        assertEquals(12, amountField.length(), "Amount field should be 12 chars");
        // 150.000 TND → 150000 in millimes → "000000150000"
        assertEquals("000000150000", amountField, "150.000 TND should be 150000 millimes (zero-padded 12)");
    }

    @Test
    void tcr0_destCurrency_position74to76() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        assertEquals("TND", line.substring(73, 76).trim(), "Currency should be TND");
    }

    @Test
    void tcr0_sourceAmount_position77to88() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        String amountField = line.substring(76, 88);
        assertEquals(12, amountField.length(), "Source amount field should be 12 chars");
        assertEquals("000000150000", amountField, "Source amount should match destination for single-currency");
    }

    @Test
    void tcr0_sourceCurrency_position89to91() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        assertEquals("TND", line.substring(88, 91).trim(), "Source currency should be TND");
    }

    @Test
    void tcr0_merchantName_position92to116() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        String nameField = line.substring(91, 116);
        assertEquals(25, nameField.length(), "Merchant name field should be 25 chars");
        assertTrue(nameField.trim().startsWith("SUPERMARCHE CARREFOUR"), "Name should contain trading name");
    }

    @Test
    void tcr0_merchantCity_position117to129() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        String cityField = line.substring(116, 129);
        assertEquals(13, cityField.length(), "City field should be 13 chars");
        assertTrue(cityField.trim().contains("TUNIS"), "City should contain merchant city");
    }

    @Test
    void tcr0_merchantCountry_is788() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        assertEquals("788", line.substring(129, 132).trim(), "Country should be 788 (Tunisia)");
    }

    @Test
    void tcr0_mcc_position133to136() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        String mccField = line.substring(132, 136);
        assertEquals(4, mccField.length(), "MCC field should be 4 chars");
        assertEquals("5411", mccField.trim(), "MCC should be 5411");
    }

    @Test
    void tcr0_merchantZip_position137to141() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        String zipField = line.substring(136, 141);
        assertEquals(5, zipField.length(), "ZIP field should be 5 chars");
        assertTrue(zipField.trim().contains("1002"), "ZIP should contain postal code");
    }

    @Test
    void tcr0_authCode_position152to157() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        String authField = line.substring(151, 157);
        assertEquals(6, authField.length(), "Auth code field should be 6 chars");
        assertTrue(authField.startsWith("AUTH12"), "Auth code should contain authorization number");
    }

    @Test
    void tcr0_additionalFields_position159to168_areSpaces() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        String additional = line.substring(158, 168);
        assertEquals(10, additional.length(), "Additional fields should be 10 chars");
        assertEquals("          ", additional, "Additional fields should be spaces (TODO)");
    }

    @Test
    void tcr0_reasonCode_is00() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        assertEquals("00", line.substring(147, 149), "Reason code should be 00");
    }

    @Test
    void tcr0_numPaymentForms_is1() {
        String line = generator.generateTc05Tcr0(sampleRecord);
        assertEquals("1", line.substring(145, 146), "Number of payment forms should be 1");
    }

    @Test
    void tcr0_generate_producesNonEmpty() {
        String result = generator.generate(CLEARING_DATE, List.of(sampleRecord));
        assertNotNull(result);
        assertFalse(result.isEmpty(), "Generated output should not be empty");
        String[] lines = result.split("\n");
        assertEquals(1, lines.length, "Should produce one line per record");
        assertEquals(168, lines[0].length(), "Each line should be 168 chars");
    }

    @Test
    void tcr0_panNotTrimmed_keepsPosition5to20() {
        ClearingRecord recordWithShortPan = ClearingRecord.builder()
                .id(UUID.randomUUID())
                .transactionId("TXN-SHORT-PAN")
                .clearingDate(CLEARING_DATE)
                .transactionDate(OffsetDateTime.now())
                .acquiringParticipantId(ACQ_ID)
                .issuingParticipantId(ISS_ID)
                .cardNumber("1234")
                .amount(new BigDecimal("50.000"))
                .currencyCode("TND")
                .mcc("0000")
                .tradingName("TEST")
                .merchantNumber("MCH0000123")
                .build();
        when(participantRepository.findById(ACQ_ID)).thenReturn(Optional.of(
                Participant.builder().id(ACQ_ID).code("BNT").bankCode("BNT01").name("Test").build()));

        String line = generator.generateTc05Tcr0(recordWithShortPan);
        // PAN at positions 5-20 should be 16 chars: "1234" left-justified + spaces
        String panField = line.substring(4, 20);
        assertEquals(16, panField.length());
        assertEquals("1234            ", panField, "Short PAN should be left-justified with spaces");
    }

    @Test
    void tcr0_unsupportedTc_returnsEmpty() {
        assertEquals("", generator.generateTc06(), "TC 06 should return empty string");
        assertEquals("", generator.generateTc07(), "TC 07 should return empty string");
        assertEquals("", generator.generateTc25(), "TC 25 should return empty string");
        assertEquals("", generator.generateTc05Tcr1(), "TC 05 TCR 1 should return empty string");
        assertEquals("", generator.generateTc05Tcr2(), "TC 05 TCR 2 should return empty string");
        assertEquals("", generator.generateTc05Tcr3(), "TC 05 TCR 3 should return empty string");
    }

    @Test
    void tcr0_noMerchant_fillsDefaults() {
        when(merchantRepository.findByMerchantId(any())).thenReturn(Optional.empty());
        when(participantRepository.findById(ISS_ID)).thenReturn(Optional.empty());

        ClearingRecord noMerchantRecord = ClearingRecord.builder()
                .id(UUID.randomUUID())
                .transactionId("TXN-NO-MERCHANT")
                .clearingDate(CLEARING_DATE)
                .transactionDate(OffsetDateTime.now())
                .acquiringParticipantId(ACQ_ID)
                .issuingParticipantId(ISS_ID)
                .cardNumber("1234567890123456")
                .amount(new BigDecimal("75.500"))
                .currencyCode("TND")
                .mcc("5812")
                .tradingName("CAFE")
                .merchantNumber("UNKNOWN")
                .build();

        String line = generator.generateTc05Tcr0(noMerchantRecord);
        assertEquals(168, line.length(), "Length should still be 168 with defaults");
        // Merchant city should be VisaBaseIISimConfig.DEFAULT_MERCHANT_CITY (13 chars)
        assertEquals("TUNIS        ", line.substring(116, 129), "City should be default TUNIS when no merchant");
        // ZIP should be VisaBaseIISimConfig.DEFAULT_MERCHANT_ZIP (5 chars)
        assertEquals("1000 ", line.substring(136, 141), "ZIP should be default 1000 when no merchant");
    }
}
