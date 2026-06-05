package com.switchplatform.platform.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, byte[]> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public KafkaEventPublisher(KafkaTemplate<String, byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public void publishTransactionReceived(TransactionReceivedEvent event) {
        publish(TopicConstants.TOPIC_TRANSACTION_RECEIVED, event.transactionId().toString(), event);
    }

    @Override
    public void publishAuthorizationApproved(AuthorizationApprovedEvent event) {
        publish(TopicConstants.TOPIC_AUTHORIZATION_APPROVED, event.transactionId().toString(), event);
    }

    @Override
    public void publishAuthorizationDeclined(AuthorizationDeclinedEvent event) {
        publish(TopicConstants.TOPIC_AUTHORIZATION_DECLINED, event.transactionId().toString(), event);
    }

    @Override
    public void publishHoldPlaced(HoldPlacedEvent event) {
        publish(TopicConstants.TOPIC_HOLD_PLACED, event.holdId().toString(), event);
    }

    @Override
    public void publishHoldReleased(HoldReleasedEvent event) {
        publish(TopicConstants.TOPIC_HOLD_RELEASED, event.holdId().toString(), event);
    }

    @Override
    public void publishSettlementInitiated(SettlementInitiatedEvent event) {
        publish(TopicConstants.TOPIC_SETTLEMENT_INITIATED, event.settlementId().toString(), event);
    }

    @Override
    public void publishSettlementCompleted(SettlementCompletedEvent event) {
        publish(TopicConstants.TOPIC_SETTLEMENT_COMPLETED, event.settlementId().toString(), event);
    }

    @Override
    public void publishClearingReceived(ClearingReceivedEvent event) {
        publish(TopicConstants.TOPIC_CLEARING_RECEIVED, event.clearingId().toString(), event);
    }

    @Override
    public void publishReconciliationTriggered(ReconciliationTriggeredEvent event) {
        publish(TopicConstants.TOPIC_RECONCILIATION_TRIGGERED, event.batchId(), event);
    }

    @Override
    public void publishFeePosted(FeePostedEvent event) {
        publish(TopicConstants.TOPIC_FEE_POSTED, event.feeId().toString(), event);
    }

    @Override
    public void publishPinVerified(PinVerifiedEvent event) {
        publish(TopicConstants.TOPIC_PIN_VERIFIED, event.cardId().toString(), event);
    }

    @Override
    public void publishPinFailed(PinFailedEvent event) {
        publish(TopicConstants.TOPIC_PIN_FAILED, event.cardId().toString(), event);
    }

    @Override
    public void publishBatchJobStarted(BatchJobStartedEvent event) {
        String topic = "EOD_CLEARING".equals(event.jobType())
                ? TopicConstants.TOPIC_BATCH_EOD_STARTED
                : TopicConstants.TOPIC_BATCH_BOD_STARTED;
        publish(topic, event.jobId().toString(), event);
    }

    @Override
    public void publishBatchJobCompleted(BatchJobCompletedEvent event) {
        String topic = "EOD_CLEARING".equals(event.jobType())
                ? TopicConstants.TOPIC_BATCH_EOD_COMPLETED
                : TopicConstants.TOPIC_BATCH_BOD_COMPLETED;
        publish(topic, event.jobId().toString(), event);
    }

    @Override
    public void publishDisputeOpened(DisputeOpenedEvent event) {
        publish(TopicConstants.TOPIC_DISPUTE_OPENED, event.disputeId().toString(), event);
    }

    @Override
    public void publishDisputeResolved(DisputeResolvedEvent event) {
        publish(TopicConstants.TOPIC_DISPUTE_RESOLVED, event.disputeId().toString(), event);
    }

    @Override
    public void publishStandInUsed(StandInUsedEvent event) {
        publish(TopicConstants.TOPIC_STANDIN_USED, event.authorizationId().toString(), event);
    }

    @Override
    public void publishClearingFileGenerated(ClearingFileGeneratedEvent event) {
        publish(TopicConstants.TOPIC_CLEARING_FILE_GENERATED, event.date(), event);
    }

    private void publish(String topic, String key, Object event) {
        try {
            byte[] payload = objectMapper.writeValueAsBytes(event);
            CompletableFuture.runAsync(() -> {
                try {
                    kafkaTemplate.send(topic, key, payload)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to publish event to topic={}, key={}, error={}",
                                        topic, key, ex.getMessage(), ex);
                            }
                        });
                } catch (Exception ex) {
                    log.error("Failed to send event to topic={}, key={}, error={}",
                            topic, key, ex.getMessage(), ex);
                }
            });
        } catch (Exception e) {
            log.error("Failed to serialize event for topic={}, key={}, error={}",
                    topic, key, e.getMessage(), e);
        }
    }
}
