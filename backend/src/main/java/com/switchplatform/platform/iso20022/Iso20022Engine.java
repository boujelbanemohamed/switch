package com.switchplatform.platform.iso20022;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
@Slf4j
public class Iso20022Engine {

    private static final String NS_PACS = "urn:iso:std:iso:20022:tech:xsd:pacs.008.001.12";
    private static final String NS_PACS_002 = "urn:iso:std:iso:20022:tech:xsd:pacs.002.001.14";
    private static final String NS_PACS_004 = "urn:iso:std:iso:20022:tech:xsd:pacs.004.001.12";
    private static final String NS_PAIN = "urn:iso:std:iso:20022:tech:xsd:pain.001.001.12";
    private static final String NS_CAMT = "urn:iso:std:iso:20022:tech:xsd:camt.053.001.10";
    private static final String NS_CAMT_052 = "urn:iso:std:iso:20022:tech:xsd:camt.052.001.10";
    private static final String NS_CAMT_054 = "urn:iso:std:iso:20022:tech:xsd:camt.054.001.10";
    private static final String NS_CAMT_056 = "urn:iso:std:iso:20022:tech:xsd:camt.056.001.10";

    private final DocumentBuilder documentBuilder;
    private final Transformer transformer;

    public Iso20022Engine() {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            this.documentBuilder = factory.newDocumentBuilder();
            this.transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialise XML parser", e);
        }
    }

    public Document parse(String xml) {
        try {
            return documentBuilder.parse(new org.xml.sax.InputSource(new StringReader(xml)));
        } catch (Exception e) {
            throw new RuntimeException("ISO 20022 XML parsing error: " + e.getMessage(), e);
        }
    }

    public String toXml(Document doc) {
        try {
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialise XML: " + e.getMessage(), e);
        }
    }

    public Document createPaymentRequest(
            String msgId, String creditorBic, String debtorBic,
            BigDecimal amount, String currency, String creditorAccount,
            String debtorAccount, String reference) {
        Document doc = documentBuilder.newDocument();
        Element root = doc.createElementNS(NS_PACS, "Document");
        doc.appendChild(root);

        Element fitfToFICstmrCdtTrf = doc.createElement("FIToFICstmrCdtTrf");
        root.appendChild(fitfToFICstmrCdtTrf);

        Element grpHdr = doc.createElement("GrpHdr");
        fitfToFICstmrCdtTrf.appendChild(grpHdr);

        appendElement(doc, grpHdr, "MsgId", msgId);
        appendElement(doc, grpHdr, "CreDtTm", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        Element nbOfTxs = appendElement(doc, grpHdr, "NbOfTxs", "1");
        Element ttlIntrBkSttlmAmt = appendElement(doc, grpHdr, "TtlIntrBkSttlmAmt", formatAmount(amount));
        ttlIntrBkSttlmAmt.setAttribute("Ccy", currency);

        appendElement(doc, grpHdr, "IntrBkSttlmDt", OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE));

        Element sttlmInf = doc.createElement("SttlmInf");
        grpHdr.appendChild(sttlmInf);
        appendElement(doc, sttlmInf, "SttlmMtd", "CLRG");

        // Credit Transfer Transaction
        Element cdtTrfTxInf = doc.createElement("CdtTrfTxInf");
        fitfToFICstmrCdtTrf.appendChild(cdtTrfTxInf);

        Element pmtId = doc.createElement("PmtId");
        cdtTrfTxInf.appendChild(pmtId);
        appendElement(doc, pmtId, "InstrId", UUID.randomUUID().toString().substring(0, 30));
        appendElement(doc, pmtId, "EndToEndId", reference);

        Element amt = doc.createElement("IntrBkSttlmAmt");
        cdtTrfTxInf.appendChild(amt);
        amt.setAttribute("Ccy", currency);
        amt.setTextContent(formatAmount(amount));

        // Debtor
        Element dbtr = doc.createElement("Dbtr");
        cdtTrfTxInf.appendChild(dbtr);
        Element dbtrAgt = doc.createElement("DbtrAgt");
        dbtr.appendChild(dbtrAgt);
        Element dbtrFinInstId = doc.createElement("FinInstnId");
        dbtrAgt.appendChild(dbtrFinInstId);
        appendElement(doc, dbtrFinInstId, "BICFI", debtorBic);

        Element dbtrAcct = doc.createElement("DbtrAcct");
        cdtTrfTxInf.appendChild(dbtrAcct);
        Element dbtrId = doc.createElement("Id");
        dbtrAcct.appendChild(dbtrId);
        appendElement(doc, dbtrId, "IBAN", debtorAccount);

        // Creditor
        Element cdtr = doc.createElement("Cdtr");
        cdtTrfTxInf.appendChild(cdtr);
        Element cdtrAgt = doc.createElement("CdtrAgt");
        cdtr.appendChild(cdtrAgt);
        Element cdtrFinInstId = doc.createElement("FinInstnId");
        cdtrAgt.appendChild(cdtrFinInstId);
        appendElement(doc, cdtrFinInstId, "BICFI", creditorBic);

        Element cdtrAcct = doc.createElement("CdtrAcct");
        cdtTrfTxInf.appendChild(cdtrAcct);
        Element cdtrId2 = doc.createElement("Id");
        cdtrAcct.appendChild(cdtrId2);
        appendElement(doc, cdtrId2, "IBAN", creditorAccount);

        if (reference != null) {
            Element rmtInf = doc.createElement("RmtInf");
            cdtTrfTxInf.appendChild(rmtInf);
            appendElement(doc, rmtInf, "Ustrd", reference);
        }

        return doc;
    }

    public Document createPaymentStatusReport(String msgId, String originalMsgId, String status) {
        Document doc = documentBuilder.newDocument();
        Element root = doc.createElementNS(NS_PACS_002, "Document");
        doc.appendChild(root);

        Element fitfToFIPmtStsRpt = doc.createElement("FIToFIPmtStsRpt");
        root.appendChild(fitfToFIPmtStsRpt);

        Element grpHdr = doc.createElement("GrpHdr");
        fitfToFIPmtStsRpt.appendChild(grpHdr);
        appendElement(doc, grpHdr, "MsgId", msgId);
        appendElement(doc, grpHdr, "CreDtTm", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        Element orgnlGrpInfAndSts = doc.createElement("OrgnlGrpInfAndSts");
        fitfToFIPmtStsRpt.appendChild(orgnlGrpInfAndSts);
        appendElement(doc, orgnlGrpInfAndSts, "OrgnlMsgId", originalMsgId);
        appendElement(doc, orgnlGrpInfAndSts, "OrgnlNbOfTxs", "1");
        appendElement(doc, orgnlGrpInfAndSts, "GrpSts", status);

        return doc;
    }

    // pacs.004: Payment Return
    public Document createPaymentReturn(String msgId, String originalMsgId,
                                         BigDecimal returnAmount, String currency,
                                         String returnReason, String additionalInfo) {
        Document doc = documentBuilder.newDocument();
        Element root = doc.createElementNS(NS_PACS_004, "Document");
        doc.appendChild(root);

        Element fitfToFIPmtRtr = doc.createElement("FIToFIPmtRtr");
        root.appendChild(fitfToFIPmtRtr);

        Element grpHdr = doc.createElement("GrpHdr");
        fitfToFIPmtRtr.appendChild(grpHdr);
        appendElement(doc, grpHdr, "MsgId", msgId);
        appendElement(doc, grpHdr, "CreDtTm", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        appendElement(doc, grpHdr, "NbOfTxs", "1");

        Element rtrInf = doc.createElement("RtrInf");
        fitfToFIPmtRtr.appendChild(rtrInf);

        Element orgnlGrpInf = doc.createElement("OrgnlGrpInf");
        rtrInf.appendChild(orgnlGrpInf);
        appendElement(doc, orgnlGrpInf, "OrgnlMsgId", originalMsgId);

        Element txInf = doc.createElement("TxInf");
        rtrInf.appendChild(txInf);

        Element rtrdAmt = doc.createElement("RtrdIntrBkSttlmAmt");
        rtrdAmt.setAttribute("Ccy", currency);
        rtrdAmt.setTextContent(formatAmount(returnAmount));
        txInf.appendChild(rtrdAmt);

        Element rtrRsnInf = doc.createElement("RtrRsnInf");
        txInf.appendChild(rtrRsnInf);
        Element rsn = doc.createElement("Rsn");
        rtrRsnInf.appendChild(rsn);
        appendElement(doc, rsn, "Cd", returnReason);
        if (additionalInfo != null) {
            appendElement(doc, rtrRsnInf, "AddtlInf", additionalInfo);
        }

        return doc;
    }

    // camt.056: Claim Non-Receipt (FIToFIPmtClm)
    public Document createClaimNonReceipt(String msgId, String originalMsgId,
                                           BigDecimal claimedAmount, String currency,
                                           String claimReason) {
        Document doc = documentBuilder.newDocument();
        Element root = doc.createElementNS(NS_CAMT_056, "Document");
        doc.appendChild(root);

        Element fitfToFIPmtClm = doc.createElement("FIToFIPmtClm");
        root.appendChild(fitfToFIPmtClm);

        Element grpHdr = doc.createElement("GrpHdr");
        fitfToFIPmtClm.appendChild(grpHdr);
        appendElement(doc, grpHdr, "MsgId", msgId);
        appendElement(doc, grpHdr, "CreDtTm", OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));

        Element clmInf = doc.createElement("ClmInf");
        fitfToFIPmtClm.appendChild(clmInf);

        Element orgnlGrpInf = doc.createElement("OrgnlGrpInf");
        clmInf.appendChild(orgnlGrpInf);
        appendElement(doc, orgnlGrpInf, "OrgnlMsgId", originalMsgId);

        Element clmAmt = doc.createElement("ClmAmt");
        clmAmt.setAttribute("Ccy", currency);
        clmAmt.setTextContent(formatAmount(claimedAmount));
        clmInf.appendChild(clmAmt);

        appendElement(doc, clmInf, "ClmRsn", claimReason);

        return doc;
    }

    public String detectMessageType(Document doc) {
        try {
            Element root = doc.getDocumentElement();
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    String name = child.getLocalName() != null ? child.getLocalName() : child.getNodeName();
                    return switch (name) {
                        case "FIToFICstmrCdtTrf" -> "pacs.008";
                        case "FIToFIPmtStsRpt" -> "pacs.002";
                        case "FIToFIPmtRtr" -> "pacs.004";
                        case "BkToCstmrStmt" -> "camt.053";
                        case "BkToCstmrAcctRpt" -> "camt.052";
                        case "BkToCstmrDbtCdtNtfctn" -> "camt.054";
                        case "FIToFIPmtClm" -> "camt.056";
                        default -> name;
                    };
                }
            }
        } catch (Exception e) {
            log.warn("Failed to detect message type: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    public Map<String, String> extractPaymentDetails(Document doc) {
        Map<String, String> result = new HashMap<>();
        try {
            String msgType = detectMessageType(doc);
            result.put("messageType", msgType);
            Element root = doc.getDocumentElement();
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    String name = child.getLocalName() != null ? child.getLocalName() : child.getNodeName();
                    switch (name) {
                        case "FIToFICstmrCdtTrf" -> extractCreditTransferDetails(child, result);
                        case "FIToFIPmtRtr" -> extractPaymentReturnDetails(child, result);
                        case "FIToFIPmtStsRpt" -> extractStatusReportDetails(child, result);
                        case "FIToFIPmtClm" -> extractClaimDetails(child, result);
                        case "BkToCstmrStmt", "BkToCstmrAcctRpt", "BkToCstmrDbtCdtNtfctn" ->
                                extractFields(child, result);
                        default -> extractFields(child, result);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to extract payment details: {}", e.getMessage());
        }
        return result;
    }

    private void extractCreditTransferDetails(Node node, Map<String, String> result) {
        result.put("transactionType", "CREDIT_TRANSFER");
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE
                    && "CdtTrfTxInf".equals(child.getLocalName() != null ? child.getLocalName() : child.getNodeName())) {
                extractFields(child, result);
            }
        }
    }

    private void extractPaymentReturnDetails(Node node, Map<String, String> result) {
        result.put("transactionType", "PAYMENT_RETURN");
        extractFields(node, result);
    }

    private void extractStatusReportDetails(Node node, Map<String, String> result) {
        result.put("transactionType", "STATUS_REPORT");
        extractFields(node, result);
    }

    private void extractClaimDetails(Node node, Map<String, String> result) {
        result.put("transactionType", "CLAIM_NON_RECEIPT");
        extractFields(node, result);
    }

    private void extractFields(Node parent, Map<String, String> result) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String name = child.getNodeName();
                String text = child.getTextContent();
                if (text != null && !text.isBlank() && child.getChildNodes().getLength() == 1) {
                    result.put(name, text.trim());
                }
                extractFields(child, result);
            }
        }
    }

    private Element appendElement(Document doc, Node parent, String name, String value) {
        Element elem = doc.createElement(name);
        elem.setTextContent(value != null ? value : "");
        parent.appendChild(elem);
        return elem;
    }

    private String formatAmount(BigDecimal amount) {
        return amount.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
