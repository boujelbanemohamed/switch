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

    private TopicConstants() {}
}
