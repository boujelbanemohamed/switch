package com.switchplatform.platform.event;

public interface EventPublisher {

    void publishTransactionReceived(TransactionReceivedEvent event);
    void publishAuthorizationApproved(AuthorizationApprovedEvent event);
    void publishAuthorizationDeclined(AuthorizationDeclinedEvent event);
    void publishHoldPlaced(HoldPlacedEvent event);
    void publishHoldReleased(HoldReleasedEvent event);
    void publishSettlementInitiated(SettlementInitiatedEvent event);
    void publishSettlementCompleted(SettlementCompletedEvent event);
    void publishClearingReceived(ClearingReceivedEvent event);
    void publishReconciliationTriggered(ReconciliationTriggeredEvent event);
    void publishFeePosted(FeePostedEvent event);
    void publishPinVerified(PinVerifiedEvent event);
    void publishPinFailed(PinFailedEvent event);

    void publishBatchJobStarted(BatchJobStartedEvent event);
    void publishBatchJobCompleted(BatchJobCompletedEvent event);
    void publishDisputeOpened(DisputeOpenedEvent event);
    void publishDisputeResolved(DisputeResolvedEvent event);

    void publishStandInUsed(StandInUsedEvent event);
}
