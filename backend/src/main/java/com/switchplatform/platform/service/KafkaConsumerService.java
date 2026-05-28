package com.switchplatform.platform.service;

import com.switchplatform.platform.model.Transaction;
import com.switchplatform.platform.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaConsumerService {

    private final SwitchCore switchCore;
    private final TransactionRepository transactionRepository;

    @KafkaListener(
            topics = "#{'${switch.mq.inbound-topics:switch-inbound}'.split(',')}",
            containerFactory = "byteArrayKafkaListenerContainerFactory"
    )
    public void onMessage(@Payload byte[] message) {
        try {
            String msgStr = new String(message, StandardCharsets.UTF_8);
            log.info("Kafka received message: {}", msgStr.length() > 200 ? msgStr.substring(0, 200) : msgStr);

            String sourceCode = "REMOTE";
            Transaction result = switchCore.processIso8583Message(message, sourceCode);
            log.info("Kafka processed transaction: id={}, status={}",
                    result.getTransactionId(), result.getStatus());
        } catch (Exception e) {
            log.error("Kafka message processing failed: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "#{'${switch.mq.inbound-xml-topics:switch-inbound-xml}'.split(',')}",
            containerFactory = "byteArrayKafkaListenerContainerFactory"
    )
    public void onXmlMessage(@Payload byte[] message) {
        try {
            String xmlMessage = new String(message, StandardCharsets.UTF_8);
            log.info("Kafka received XML message: {}", xmlMessage.length() > 200 ? xmlMessage.substring(0, 200) : xmlMessage);
            String sourceCode = "REMOTE";
            Transaction result = switchCore.processIso20022Message(xmlMessage, sourceCode);
            log.info("Kafka processed XML transaction: id={}, status={}",
                    result.getTransactionId(), result.getStatus());
        } catch (Exception e) {
            log.error("Kafka XML message processing failed: {}", e.getMessage(), e);
        }
    }
}
