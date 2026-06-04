package com.switchplatform.platform.event;

public final class TopicConstants {

    public static final String TOPIC_TRANSACTION_RECEIVED = "switch.transaction.received";
    public static final String TOPIC_AUTHORIZATION_APPROVED = "switch.authorization.approved";
    public static final String TOPIC_AUTHORIZATION_DECLINED = "switch.authorization.declined";
    public static final String TOPIC_HOLD_PLACED = "switch.hold.placed";
    public static final String TOPIC_HOLD_RELEASED = "switch.hold.released";
    public static final String TOPIC_SETTLEMENT_INITIATED = "switch.settlement.initiated";
    public static final String TOPIC_SETTLEMENT_COMPLETED = "switch.settlement.completed";
    public static final String TOPIC_CLEARING_RECEIVED = "switch.clearing.received";
    public static final String TOPIC_RECONCILIATION_TRIGGERED = "switch.reconciliation.triggered";
    public static final String TOPIC_FEE_POSTED = "switch.fee.posted";
    public static final String TOPIC_PIN_VERIFIED = "switch.pin.verified";
    public static final String TOPIC_PIN_FAILED = "switch.pin.failed";
    public static final String TOPIC_DLQ = "switch.dlq";

    // Batch topics
    public static final String TOPIC_BATCH_EOD_STARTED = "switch.batch.eod.started";
    public static final String TOPIC_BATCH_EOD_COMPLETED = "switch.batch.eod.completed";
    public static final String TOPIC_BATCH_BOD_STARTED = "switch.batch.bod.started";
    public static final String TOPIC_BATCH_BOD_COMPLETED = "switch.batch.bod.completed";

    // Dispute topics
    public static final String TOPIC_DISPUTE_OPENED = "switch.dispute.opened";
    public static final String TOPIC_DISPUTE_RESOLVED = "switch.dispute.resolved";
    public static final String TOPIC_DISPUTE_TRANSITIONED = "switch.dispute.transitioned";

    // Netting topic
    public static final String TOPIC_NETTING_CALCULATED = "switch.netting.calculated";

    // KYC topic
    public static final String TOPIC_KYC_UPDATED = "switch.kyc.updated";

    // Virtual card topic
    public static final String TOPIC_VIRTUAL_CARD_ISSUED = "switch.virtual_card.issued";

    // Stand-in topic
    public static final String TOPIC_STANDIN_USED = "switch.standin.used";

    private TopicConstants() {}
}
