package com.switchplatform.platform.iso8583;

import com.solab.iso8583.IsoMessage;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.IsoValue;
import com.solab.iso8583.MessageFactory;
import com.solab.iso8583.parse.ConfigParser;
import com.solab.iso8583.parse.FieldParseInfo;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@Component
@Slf4j
public class Iso8583Engine {

    private final MessageFactory<IsoMessage> messageFactory;

    public Iso8583Engine() {
        this.messageFactory = new MessageFactory<>();
        this.messageFactory.setCharacterEncoding("UTF-8");
        this.messageFactory.setForceSecondaryBitmap(true);
        this.messageFactory.setUseBinaryMessages(true);
    }

    @PostConstruct
    public void init() {
        try {
            ConfigParser.configureFromClasspathConfig(messageFactory, "iso8583-config.xml");
        } catch (IOException e) {
            log.warn("Could not load ISO 8583 config from classpath, using defaults: {}", e.getMessage());
        }
        registerParseGuides();
        log.info("ISO 8583 MessageFactory initialised");
    }

    private void registerParseGuides() {
        // j8583 binary reads/writes MTI as 2-byte big-endian integer value:
        // "0200" -> int 200 -> bytes 0x00, 0xC8
        int[][] mtisAndFields = {
            {200, 2, 3, 4, 7, 11, 12, 13, 18, 22, 35, 37, 41, 42, 49},
            {210, 2, 3, 4, 5, 7, 11, 12, 13, 18, 22, 35, 37, 38, 39, 41, 42, 49},
            {100, 2, 3, 4, 7, 11, 12, 13, 18, 22, 35, 37, 41, 42, 49},
            {220, 2, 3, 4, 7, 11, 12, 13, 18, 22, 35, 37, 41, 42, 49},
            {400, 2, 3, 4, 7, 11, 12, 18, 37, 90},
            {420, 2, 3, 4, 7, 11, 12, 18, 37, 90},
        };
        for (int[] mtiAndFields : mtisAndFields) {
            int mti = mtiAndFields[0];
            Map<Integer, FieldParseInfo> map = new HashMap<>();
            for (int i = 1; i < mtiAndFields.length; i++) {
                int f = mtiAndFields[i];
                map.put(f, parserForField(f));
            }
            messageFactory.setParseMap(mti, map);
        }
    }

    private FieldParseInfo parserForField(int field) {
        String enc = messageFactory.getCharacterEncoding();
        return switch (field) {
            case 2 -> FieldParseInfo.getInstance(IsoType.LLVAR, 19, enc);
            case 3 -> FieldParseInfo.getInstance(IsoType.NUMERIC, 6, enc);
            case 4 -> FieldParseInfo.getInstance(IsoType.AMOUNT, 12, enc);
            case 5 -> FieldParseInfo.getInstance(IsoType.AMOUNT, 12, enc);
            case 6 -> FieldParseInfo.getInstance(IsoType.AMOUNT, 12, enc);
            case 7 -> FieldParseInfo.getInstance(IsoType.DATE10, 10, enc);
            case 11 -> FieldParseInfo.getInstance(IsoType.NUMERIC, 6, enc);
            case 12 -> FieldParseInfo.getInstance(IsoType.TIME, 6, enc);
            case 13 -> FieldParseInfo.getInstance(IsoType.DATE4, 4, enc);
            case 18 -> FieldParseInfo.getInstance(IsoType.NUMERIC, 4, enc);
            case 22 -> FieldParseInfo.getInstance(IsoType.NUMERIC, 3, enc);
            case 25 -> FieldParseInfo.getInstance(IsoType.NUMERIC, 2, enc);
            case 35 -> FieldParseInfo.getInstance(IsoType.LLVAR, 37, enc);
            case 37 -> FieldParseInfo.getInstance(IsoType.ALPHA, 12, enc);
            case 41 -> FieldParseInfo.getInstance(IsoType.ALPHA, 8, enc);
            case 42 -> FieldParseInfo.getInstance(IsoType.ALPHA, 15, enc);
            case 38 -> FieldParseInfo.getInstance(IsoType.ALPHA, 6, enc);
            case 39 -> FieldParseInfo.getInstance(IsoType.ALPHA, 2, enc);
            case 49 -> FieldParseInfo.getInstance(IsoType.ALPHA, 3, enc);
            case 90 -> FieldParseInfo.getInstance(IsoType.LLVAR, 42, enc);
            default -> throw new IllegalArgumentException("Unknown field: " + field);
        };
    }

    public IsoMessage parse(byte[] data) {
        try {
            IsoMessage msg = messageFactory.parseMessage(data, 0);
            if (msg == null) {
                throw new IllegalArgumentException("Failed to parse ISO 8583 message");
            }
            return msg;
        } catch (IOException | ParseException e) {
            throw new RuntimeException("ISO 8583 parsing error: " + e.getMessage(), e);
        }
    }

    public IsoMessage parse(String data) {
        return parse(data.getBytes());
    }

    public byte[] encode(IsoMessage message) {
        byte[] data = message.writeData();
        log.debug("Encoded ISO 8583 message ({} bytes, MTI: {})", data.length, message.getType());
        return data;
    }

    public IsoMessage createMessage(String mti) {
        IsoMessage msg = messageFactory.newMessage(Integer.parseInt(mti));
        if (msg == null) {
            IsoMessage m = new IsoMessage();
            m.setType(Integer.parseInt(mti));
            return m;
        }
        return msg;
    }

    public IsoMessage createAuthorizationRequest(
            String pan, BigDecimal amount, String currencyCode,
            String stan, String merchantId, String terminalId) {
        return createAuthorizationRequest(pan, amount, currencyCode, stan, merchantId, terminalId, null);
    }

    public IsoMessage createAuthorizationRequest(
            String pan, BigDecimal amount, String currencyCode,
            String stan, String merchantId, String terminalId, String mcc) {
        IsoMessage msg = createMessage("0200");
        msg.setValue(2, pan, IsoType.LLVAR, 19);
        msg.setValue(3, "003000", IsoType.NUMERIC, 6);
        msg.setValue(4, formatAmount(amount), IsoType.AMOUNT, 12);
        msg.setValue(7, formatDate(new Date()), IsoType.DATE10, 10);
        msg.setValue(11, stan, IsoType.NUMERIC, 6);
        msg.setValue(12, formatTime(new Date()), IsoType.TIME, 6);
        msg.setValue(13, formatMonthDay(new Date()), IsoType.DATE4, 4);
        if (mcc != null) {
            msg.setValue(18, mcc, IsoType.NUMERIC, 4);
        }
        msg.setValue(22, "051", IsoType.NUMERIC, 3);
        msg.setValue(35, pan, IsoType.LLVAR, 37);
        msg.setValue(37, generateRrn(), IsoType.ALPHA, 12);
        msg.setValue(41, terminalId, IsoType.ALPHA, 8);
        msg.setValue(42, merchantId, IsoType.ALPHA, 15);
        msg.setValue(49, currencyCode, IsoType.ALPHA, 3);
        return msg;
    }

    public IsoMessage createAuthorizationResponse(IsoMessage request, String responseCode) {
        IsoMessage response = createMessage("0210");
        copyFields(response, request, 2, 3, 4, 7, 11, 12, 13, 22, 35, 37, 41, 42, 49);
        response.setValue(39, responseCode, IsoType.ALPHA, 2);
        return response;
    }

    public IsoMessage createReversalRequest(
            String pan, BigDecimal amount, String stan,
            String originalStan, String rrn) {
        IsoMessage msg = createMessage("0400");
        msg.setValue(2, pan, IsoType.LLVAR, 19);
        msg.setValue(3, "003000", IsoType.NUMERIC, 6);
        msg.setValue(4, formatAmount(amount), IsoType.AMOUNT, 12);
        msg.setValue(7, formatDate(new Date()), IsoType.DATE10, 10);
        msg.setValue(11, stan, IsoType.NUMERIC, 6);
        msg.setValue(12, formatTime(new Date()), IsoType.TIME, 6);
        msg.setValue(37, rrn, IsoType.ALPHA, 12);
        msg.setValue(90, originalStan + rrn, IsoType.LLVAR, 42);
        return msg;
    }

    public IsoMessage createFinancialRequest(
            String pan, BigDecimal amount, String currencyCode,
            String stan, String merchantId, String terminalId) {
        IsoMessage msg = createMessage("0100");
        msg.setValue(2, pan, IsoType.LLVAR, 19);
        msg.setValue(3, "003000", IsoType.NUMERIC, 6);
        msg.setValue(4, formatAmount(amount), IsoType.AMOUNT, 12);
        msg.setValue(7, formatDate(new Date()), IsoType.DATE10, 10);
        msg.setValue(11, stan, IsoType.NUMERIC, 6);
        msg.setValue(12, formatTime(new Date()), IsoType.TIME, 6);
        msg.setValue(13, formatMonthDay(new Date()), IsoType.DATE4, 4);
        msg.setValue(22, "051", IsoType.NUMERIC, 3);
        msg.setValue(35, pan, IsoType.LLVAR, 37);
        msg.setValue(37, generateRrn(), IsoType.ALPHA, 12);
        msg.setValue(41, terminalId, IsoType.ALPHA, 8);
        msg.setValue(42, merchantId, IsoType.ALPHA, 15);
        msg.setValue(49, currencyCode, IsoType.ALPHA, 3);
        return msg;
    }

    public IsoMessage createAuthorizationAdvice(
            String pan, BigDecimal amount, String currencyCode,
            String stan, String merchantId, String terminalId) {
        IsoMessage msg = createMessage("0220");
        msg.setValue(2, pan, IsoType.LLVAR, 19);
        msg.setValue(3, "003000", IsoType.NUMERIC, 6);
        msg.setValue(4, formatAmount(amount), IsoType.AMOUNT, 12);
        msg.setValue(7, formatDate(new Date()), IsoType.DATE10, 10);
        msg.setValue(11, stan, IsoType.NUMERIC, 6);
        msg.setValue(12, formatTime(new Date()), IsoType.TIME, 6);
        msg.setValue(13, formatMonthDay(new Date()), IsoType.DATE4, 4);
        msg.setValue(22, "051", IsoType.NUMERIC, 3);
        msg.setValue(35, pan, IsoType.LLVAR, 37);
        msg.setValue(37, generateRrn(), IsoType.ALPHA, 12);
        msg.setValue(41, terminalId, IsoType.ALPHA, 8);
        msg.setValue(42, merchantId, IsoType.ALPHA, 15);
        msg.setValue(49, currencyCode, IsoType.ALPHA, 3);
        return msg;
    }

    public IsoMessage createAuthorizationAdviceResponse(IsoMessage request, String responseCode) {
        IsoMessage response = createMessage("0230");
        copyFields(response, request, 2, 3, 4, 7, 11, 12, 13, 22, 35, 37, 41, 42, 49);
        response.setValue(39, responseCode, IsoType.ALPHA, 2);
        return response;
    }

    public IsoMessage createReversalAdvice(
            String pan, BigDecimal amount, String stan,
            String originalStan, String rrn) {
        IsoMessage msg = createMessage("0420");
        msg.setValue(2, pan, IsoType.LLVAR, 19);
        msg.setValue(3, "003000", IsoType.NUMERIC, 6);
        msg.setValue(4, formatAmount(amount), IsoType.AMOUNT, 12);
        msg.setValue(7, formatDate(new Date()), IsoType.DATE10, 10);
        msg.setValue(11, stan, IsoType.NUMERIC, 6);
        msg.setValue(12, formatTime(new Date()), IsoType.TIME, 6);
        msg.setValue(37, rrn, IsoType.ALPHA, 12);
        msg.setValue(90, originalStan + rrn, IsoType.LLVAR, 42);
        return msg;
    }

    public IsoMessage createReversalAdviceResponse(IsoMessage request, String responseCode) {
        IsoMessage response = createMessage("0430");
        copyFields(response, request, 2, 3, 4, 7, 11, 12, 13, 37, 90);
        response.setValue(39, responseCode, IsoType.ALPHA, 2);
        return response;
    }

    public IsoMessage createNetworkManagementRequest(String functionCode) {
        IsoMessage msg = createMessage("0800");
        msg.setValue(7, formatDate(new Date()), IsoType.DATE10, 10);
        msg.setValue(11, String.format("%06d", (int)(Math.random() * 999999)), IsoType.NUMERIC, 6);
        msg.setValue(12, formatTime(new Date()), IsoType.TIME, 6);
        msg.setValue(13, formatMonthDay(new Date()), IsoType.DATE4, 4);
        msg.setValue(70, functionCode, IsoType.NUMERIC, 3);
        return msg;
    }

    public IsoMessage createNetworkManagementResponse(IsoMessage request, String responseCode) {
        IsoMessage response = createMessage("0810");
        copyFields(response, request, 7, 11, 12, 13, 70);
        response.setValue(39, responseCode, IsoType.ALPHA, 2);
        return response;
    }

    public Map<String, Object> toMap(IsoMessage msg) {
        Map<String, Object> result = new HashMap<>();
        result.put("mti", String.format("%04d", msg.getType()));
        for (int i = 2; i <= 128; i++) {
            IsoValue<?> value = msg.getField(i);
            if (value != null) {
                result.put("field_" + i, value.getValue());
            }
        }
        return result;
    }

    private void copyFields(IsoMessage target, IsoMessage source, int... fields) {
        for (int field : fields) {
            IsoValue<?> value = source.getField(field);
            if (value != null) {
                target.setField(field, value);
            }
        }
    }

    private String formatAmount(BigDecimal amount) {
        return String.format("%012d", amount.multiply(new BigDecimal(100)).longValue());
    }

    private String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMddHHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    private String formatTime(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(date);
    }

    private String formatMonthDay(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMdd");
        return sdf.format(date);
    }

    private String generateRrn() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyMMddHHmmssSSS");
        return sdf.format(new Date()) + (int)(Math.random() * 1000);
    }

    public MessageFactory<IsoMessage> getMessageFactory() {
        return messageFactory;
    }
}
