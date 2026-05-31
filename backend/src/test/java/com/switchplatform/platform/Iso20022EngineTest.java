package com.switchplatform.platform;

import com.switchplatform.platform.iso20022.Iso20022Engine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Iso20022EngineTest {

    private Iso20022Engine engine;

    @BeforeEach
    void setUp() {
        engine = new Iso20022Engine();
    }

    @Test
    void shouldParseValidXml() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.12\">"
                + "<FIToFICstmrCdtTrf><GrpHdr><MsgId>MSG001</MsgId></GrpHdr></FIToFICstmrCdtTrf>"
                + "</Document>";
        Document doc = engine.parse(xml);
        assertNotNull(doc);
        assertEquals("Document", doc.getDocumentElement().getLocalName());
    }

    @Test
    void shouldThrowOnInvalidXml() {
        assertThrows(RuntimeException.class, () -> engine.parse("not xml"));
    }

    @Test
    void shouldCreatePaymentRequest() {
        Document doc = engine.createPaymentRequest(
                "REF001", "CREDBICXX", "DEBTBICYY",
                BigDecimal.valueOf(250.00), "TND",
                "TN5901001234567890", "TN5902009876543210",
                "INVOICE-2026-001");

        assertNotNull(doc);
        String xml = engine.toXml(doc);
        assertTrue(xml.contains("REF001"));
        assertTrue(xml.contains("CREDBICXX"));
        assertTrue(xml.contains("DEBTBICYY"));
        assertTrue(xml.contains("250.00"));
        assertTrue(xml.contains("TND"));
        assertTrue(xml.contains("INVOICE-2026-001"));
    }

    @Test
    void shouldCreatePaymentStatusReport() {
        Document doc = engine.createPaymentStatusReport(
                "STS001", "ORIG001", "ACCP");

        assertNotNull(doc);
        String xml = engine.toXml(doc);
        assertTrue(xml.contains("STS001"));
        assertTrue(xml.contains("ORIG001"));
        assertTrue(xml.contains("ACCP"));
    }

    @Test
    void shouldExtractPaymentDetails() {
        Document doc = engine.createPaymentRequest(
                "EXT001", "CREDBIC", "DEBTBIC",
                BigDecimal.valueOf(500.00), "EUR",
                "FR761234567890", "DE761234567890",
                "PAYMENT-REF");

        Map<String, String> details = engine.extractPaymentDetails(doc);
        assertFalse(details.isEmpty());
        assertEquals("CREDIT_TRANSFER", details.get("transactionType"));
    }

    @Test
    void shouldRoundTripPaymentRequest() {
        Document original = engine.createPaymentRequest(
                "ROUND01", "CREDBIC", "DEBTBIC",
                BigDecimal.valueOf(1000.00), "USD",
                "US123456789", "US987654321",
                "ROUND-TRIP");

        String xml = engine.toXml(original);
        Document parsed = engine.parse(xml);
        assertNotNull(parsed);

        Map<String, String> details = engine.extractPaymentDetails(parsed);
        assertEquals("CREDIT_TRANSFER", details.get("transactionType"));
    }

    @Test
    void shouldFormatAmountWithTwoDecimals() {
        Document doc = engine.createPaymentRequest(
                "AMT001", "CREDBIC", "DEBTBIC",
                BigDecimal.valueOf(99.5), "TND",
                "TN5901000001", "TN5902000002",
                null);

        String xml = engine.toXml(doc);
        assertTrue(xml.contains("99.50"));
    }

    @Test
    void shouldSupportMultiplePaymentReferences() {
        Document docWithRef = engine.createPaymentRequest(
                "MREF01", "CREDBIC", "DEBTBIC",
                BigDecimal.valueOf(75.00), "TND",
                "TN5901000001", "TN5902000002",
                "REF-123");

        String xml = engine.toXml(docWithRef);
        assertTrue(xml.contains("REF-123"));

        Document docWithoutRef = engine.createPaymentRequest(
                "MREF02", "CREDBIC", "DEBTBIC",
                BigDecimal.valueOf(30.00), "EUR",
                "FR7612345678", "DE7612345678",
                null);

        String xml2 = engine.toXml(docWithoutRef);
        assertTrue(xml2.contains("MREF02"));
        assertTrue(xml2.contains("30.00"));
    }

    @Test
    void shouldCreateAndParsePaymentStatusReport() {
        Document doc = engine.createPaymentStatusReport(
                "STS-ROUND-01", "ORIG-ROUND-01", "RJCT");

        String xml = engine.toXml(doc);
        Document parsed = engine.parse(xml);

        assertNotNull(parsed);
        String xml2 = engine.toXml(parsed);
        assertTrue(xml2.contains("STS-ROUND-01"));
        assertTrue(xml2.contains("RJCT"));
    }

    @Test
    void shouldDetectMessageTypeForNonPaymentDocument() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:camt.053.001.10\">"
                + "<BkToCstmrStmt><Stmt><Id>STMT001</Id></Stmt></BkToCstmrStmt>"
                + "</Document>";
        Document doc = engine.parse(xml);
        assertEquals("camt.053", engine.detectMessageType(doc));
        Map<String, String> details = engine.extractPaymentDetails(doc);
        assertFalse(details.isEmpty());
        assertEquals("camt.053", details.get("messageType"));
    }

    @Test
    void shouldSerializeToXml() {
        Document doc = engine.createPaymentRequest(
                "SER001", "CREDBIC", "DEBTBIC",
                BigDecimal.valueOf(10.00), "TND",
                "TN5901000001", "TN5902000002",
                "SERIALIZE-TEST");

        String xml = engine.toXml(doc);
        assertNotNull(xml);
        assertFalse(xml.isEmpty());
        assertTrue(xml.startsWith("<?xml"));
        assertTrue(xml.contains("</Document>"));
    }
}
