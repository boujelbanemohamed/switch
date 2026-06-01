package com.switchplatform.platform.event;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaTopicConfig {

    private NewTopic createTopic(String name) {
        return TopicBuilder.name(name)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic topicTransactionReceived() {
        return createTopic(TopicConstants.TOPIC_TRANSACTION_RECEIVED);
    }

    @Bean
    public NewTopic topicAuthorizationApproved() {
        return createTopic(TopicConstants.TOPIC_AUTHORIZATION_APPROVED);
    }

    @Bean
    public NewTopic topicAuthorizationDeclined() {
        return createTopic(TopicConstants.TOPIC_AUTHORIZATION_DECLINED);
    }

    @Bean
    public NewTopic topicHoldPlaced() {
        return createTopic(TopicConstants.TOPIC_HOLD_PLACED);
    }

    @Bean
    public NewTopic topicHoldReleased() {
        return createTopic(TopicConstants.TOPIC_HOLD_RELEASED);
    }

    @Bean
    public NewTopic topicSettlementInitiated() {
        return createTopic(TopicConstants.TOPIC_SETTLEMENT_INITIATED);
    }

    @Bean
    public NewTopic topicSettlementCompleted() {
        return createTopic(TopicConstants.TOPIC_SETTLEMENT_COMPLETED);
    }

    @Bean
    public NewTopic topicClearingReceived() {
        return createTopic(TopicConstants.TOPIC_CLEARING_RECEIVED);
    }

    @Bean
    public NewTopic topicReconciliationTriggered() {
        return createTopic(TopicConstants.TOPIC_RECONCILIATION_TRIGGERED);
    }

    @Bean
    public NewTopic topicFeePosted() {
        return createTopic(TopicConstants.TOPIC_FEE_POSTED);
    }

    @Bean
    public NewTopic topicPinVerified() {
        return createTopic(TopicConstants.TOPIC_PIN_VERIFIED);
    }

    @Bean
    public NewTopic topicPinFailed() {
        return createTopic(TopicConstants.TOPIC_PIN_FAILED);
    }

    @Bean
    public NewTopic topicDlq() {
        return TopicBuilder.name(TopicConstants.TOPIC_DLQ)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
