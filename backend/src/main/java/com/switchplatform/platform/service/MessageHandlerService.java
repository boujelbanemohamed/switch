package com.switchplatform.platform.service;

import com.switchplatform.platform.model.DLQRecord;
import com.switchplatform.platform.model.Participant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
@RequiredArgsConstructor
public class MessageHandlerService {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final ConcurrentHashMap<UUID, DLQRecord> deadLetterQueue = new ConcurrentHashMap<>();

    @Value("${switch.mq.request-timeout:5000}")
    private long requestTimeoutMs;

    @Value("${switch.mq.retry-backoff:100,500,2000}")
    private String retryBackoffConfig;

    private long[] retryDelaysMs;
    private static final int MAX_RETRIES = 3;
    private static final long SEND_TIMEOUT_SECONDS = 5;

    private long[] parseRetryBackoff() {
        if (retryBackoffConfig == null || retryBackoffConfig.isBlank()) {
            return new long[]{100L, 500L, 2000L};
        }
        String[] parts = retryBackoffConfig.split(",");
        long[] delays = new long[parts.length];
        for (int i = 0; i < parts.length; i++) {
            delays[i] = Long.parseLong(parts[i].trim());
        }
        return delays;
    }

    public byte[] sendAndReceive(Participant destination, byte[] message) {
        return switch (destination.getEndpointType()) {
            case TCP -> sendTcp(destination.getEndpointUrl(), message);
            case HTTP -> sendHttp(destination.getEndpointUrl(), message);
            case MQ -> sendMq(destination, message);
            case FILE -> sendFile(destination, message);
        };
    }

    public String sendAndReceiveXml(Participant destination, String xmlMessage) {
        byte[] response = sendAndReceive(destination,
                xmlMessage.getBytes(StandardCharsets.UTF_8));
        return new String(response, StandardCharsets.UTF_8);
    }

    public List<DLQRecord> getDeadLetterQueue() {
        return new ArrayList<>(deadLetterQueue.values());
    }

    public boolean retryDlqMessage(UUID dlqId) {
        DLQRecord record = deadLetterQueue.get(dlqId);
        if (record == null) {
            return false;
        }
        try {
            kafkaTemplate.send(record.topic(), dlqId.toString(), record.payload().getBytes(StandardCharsets.UTF_8))
                    .get(requestTimeoutMs > 0 ? requestTimeoutMs / 1000 : SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            deadLetterQueue.remove(dlqId);
            log.info("DLQ message {} retried successfully to topic={}", dlqId, record.topic());
            return true;
        } catch (Exception e) {
            DLQRecord updated = new DLQRecord(
                    record.id(), record.topic(), record.payload(),
                    e.getMessage(), record.retryCount() + 1,
                    record.createdAt(), OffsetDateTime.now()
            );
            deadLetterQueue.put(dlqId, updated);
            log.error("DLQ retry failed for {}: {}", dlqId, e.getMessage());
            return false;
        }
    }

    public int getDlqCount() {
        return deadLetterQueue.size();
    }

    public Map<String, Object> getMqStatus() {
        return Map.of(
                "kafkaEnabled", true,
                "dlqCount", deadLetterQueue.size(),
                "status", "available"
        );
    }

    private byte[] sendTcp(String endpointUrl, byte[] message) {
        try {
            String[] parts = endpointUrl.split(":");
            String host = parts[0];
            int port = Integer.parseInt(parts[1]);

            try (Socket socket = new Socket(host, port);
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                dos.writeInt(message.length);
                dos.write(message);
                dos.flush();

                int length = dis.readInt();
                byte[] response = new byte[length];
                dis.readFully(response);
                return response;
            }
        } catch (IOException e) {
            throw new RuntimeException("TCP communication failed: " + e.getMessage(), e);
        }
    }

    private byte[] sendHttp(String endpointUrl, byte[] message) {
        try {
            URL url = URI.create(endpointUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setDoOutput(true);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(message);
                os.flush();
            }

            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HTTP error: " + responseCode);
            }

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                InputStream is = conn.getInputStream();
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toByteArray();
            }
        } catch (IOException e) {
            throw new RuntimeException("HTTP communication failed: " + e.getMessage(), e);
        }
    }

    private byte[] sendMq(Participant destination, byte[] message) {
        String topic = destination.getEndpointUrl() != null
                ? destination.getEndpointUrl() : "switch-mq-" + destination.getCode();
        String correlationId = UUID.randomUUID().toString();
        String payloadStr = new String(message, StandardCharsets.UTF_8);

        Exception lastException = null;

        long[] delays = retryDelaysMs;
        if (delays == null) {
            delays = parseRetryBackoff();
            retryDelaysMs = delays;
        }
        long timeoutSeconds = requestTimeoutMs > 0 ? requestTimeoutMs / 1000 : SEND_TIMEOUT_SECONDS;

        for (int attempt = 0; attempt <= MAX_RETRIES; attempt++) {
            try {
                CompletableFuture<SendResult<String, byte[]>> future =
                        kafkaTemplate.send(topic, correlationId, message);
                future.get(timeoutSeconds, TimeUnit.SECONDS);
                log.info("MQ Kafka: message sent to topic={}, correlationId={}, attempt={}",
                        topic, correlationId, attempt + 1);
                return "OK".getBytes(StandardCharsets.UTF_8);
            } catch (TimeoutException e) {
                lastException = e;
                log.warn("MQ Kafka send attempt {} timed out for topic={}, timeout={}s", attempt + 1, topic, timeoutSeconds);
            } catch (ExecutionException e) {
                lastException = e;
                log.warn("MQ Kafka send attempt {} failed for topic={}: {}",
                        attempt + 1, topic, e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            } catch (CancellationException | InterruptedException e) {
                lastException = e;
                Thread.currentThread().interrupt();
                log.warn("MQ Kafka send attempt {} interrupted for topic={}", attempt + 1, topic);
                break;
            }

            if (attempt < MAX_RETRIES && attempt < delays.length) {
                try {
                    Thread.sleep(delays[attempt]);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        DLQRecord dlq = new DLQRecord(
                UUID.randomUUID(),
                topic,
                payloadStr,
                lastException != null ? lastException.getMessage() : "Unknown error",
                MAX_RETRIES,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        deadLetterQueue.put(dlq.id(), dlq);
        log.error("MQ Kafka send failed after {} retries, stored in DLQ: id={}, topic={}",
                MAX_RETRIES, dlq.id(), topic);
        return "ERROR".getBytes(StandardCharsets.UTF_8);
    }

    private byte[] sendFile(Participant destination, byte[] message) {
        try {
            String filePath = destination.getEndpointUrl() + "/" +
                    System.currentTimeMillis() + ".msg";
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(message);
            }
            log.info("Message written to file: {}", filePath);
            return "OK".getBytes();
        } catch (IOException e) {
            throw new RuntimeException("File write failed: " + e.getMessage(), e);
        }
    }
}
