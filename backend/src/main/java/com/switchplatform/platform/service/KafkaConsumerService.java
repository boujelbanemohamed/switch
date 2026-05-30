package com.switchplatform.platform.service;

import com.switchplatform.platform.model.Transaction;
import com.switchplatform.platform.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
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
    public void onMessage(
            @Payload byte[] message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        try {
            String msgStr = new String(message, StandardCharsets.UTF_8);
            log.info("Kafka consumed: topic={}, partition={}, offset={}, key={}, length={}",
                    topic, partition, offset, key, message.length);

            String sourceCode = "REMOTE";
            Transaction result = switchCore.processIso8583Message(message, sourceCode);
            log.info("Kafka processed transaction: id={}, status={}, responseCode={}",
                    result.getTransactionId(), result.getStatus(), result.getResponseCode());

        } catch (Exception e) {
            log.error("Kafka message processing failed: topic={}, partition={}, offset={}, error={}",
                    topic, partition, offset, e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = "#{'${switch.mq.inbound-xml-topics:switch-inbound-xml}'.split(',')}",
            containerFactory = "byteArrayKafkaListenerContainerFactory"
    )
    public void onXmlMessage(
            @Payload byte[] message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            Acknowledgment acknowledgment) {
        try {
            String xmlMessage = new String(message, StandardCharsets.UTF_8);
            log.info("Kafka consumed XML: topic={}, partition={}, offset={}, key={}, length={}",
                    topic, partition, offset, key, xmlMessage.length());

            String sourceCode = "REMOTE";
            Transaction result = switchCore.processIso20022Message(xmlMessage, sourceCode);
            log.info("Kafka processed XML transaction: id={}, status={}, responseCode={}",
                    result.getTransactionId(), result.getStatus(), result.getResponseCode());

            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Kafka XML message processing failed: topic={}, partition={}, offset={}, error={}",
                    topic, partition, offset, e.getMessage(), e);
        }
    }
}
