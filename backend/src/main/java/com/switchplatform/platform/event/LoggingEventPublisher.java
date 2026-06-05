package com.switchplatform.platform.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@ConditionalOnMissingBean(EventPublisher.class)
public class LoggingEventPublisher implements EventPublisher {

    @Override
    public void publishTransactionReceived(TransactionReceivedEvent event) {
        log.debug("Event: transactionReceived - {}", event);
    }

    @Override
    public void publishAuthorizationApproved(AuthorizationApprovedEvent event) {
        log.debug("Event: authorizationApproved - {}", event);
    }

    @Override
    public void publishAuthorizationDeclined(AuthorizationDeclinedEvent event) {
        log.debug("Event: authorizationDeclined - {}", event);
    }

    @Override
    public void publishHoldPlaced(HoldPlacedEvent event) {
        log.debug("Event: holdPlaced - {}", event);
    }

    @Override
    public void publishHoldReleased(HoldReleasedEvent event) {
        log.debug("Event: holdReleased - {}", event);
    }

    @Override
    public void publishSettlementInitiated(SettlementInitiatedEvent event) {
        log.debug("Event: settlementInitiated - {}", event);
    }

    @Override
    public void publishSettlementCompleted(SettlementCompletedEvent event) {
        log.debug("Event: settlementCompleted - {}", event);
    }

    @Override
    public void publishClearingReceived(ClearingReceivedEvent event) {
        log.debug("Event: clearingReceived - {}", event);
    }

    @Override
    public void publishReconciliationTriggered(ReconciliationTriggeredEvent event) {
        log.debug("Event: reconciliationTriggered - {}", event);
    }

    @Override
    public void publishFeePosted(FeePostedEvent event) {
        log.debug("Event: feePosted - {}", event);
    }

    @Override
    public void publishPinVerified(PinVerifiedEvent event) {
        log.debug("Event: pinVerified - {}", event);
    }

    @Override
    public void publishPinFailed(PinFailedEvent event) {
        log.debug("Event: pinFailed - {}", event);
    }

    @Override
    public void publishBatchJobStarted(BatchJobStartedEvent event) {
        log.debug("Event: batchJobStarted - {}", event);
    }

    @Override
    public void publishBatchJobCompleted(BatchJobCompletedEvent event) {
        log.debug("Event: batchJobCompleted - {}", event);
    }

    @Override
    public void publishDisputeOpened(DisputeOpenedEvent event) {
        log.debug("Event: disputeOpened - {}", event);
    }

    @Override
    public void publishDisputeResolved(DisputeResolvedEvent event) {
        log.debug("Event: disputeResolved - {}", event);
    }

    @Override
    public void publishStandInUsed(StandInUsedEvent event) {
        log.debug("Event: standInUsed - {}", event);
    }

    @Override
    public void publishClearingFileGenerated(ClearingFileGeneratedEvent event) {
        log.debug("Event: clearingFileGenerated - {}", event);
    }
}
