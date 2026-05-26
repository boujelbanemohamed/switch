package com.switchplatform.platform.model.ecommerce;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "three_ds_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThreeDsSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "epg_transaction_id")
    private UUID epgTransactionId;

    @Column(name = "transaction_id", nullable = false, length = 64)
    private String transactionId;

    @Column(name = "card_id")
    private UUID cardId;

    @Column(name = "three_ds_version", length = 10)
    private String threeDsVersion;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "authentication_type", length = 20)
    private String authenticationType;

    @Column(name = "acs_reference_number", length = 64)
    private String acsReferenceNumber;

    @Column(name = "ds_reference_number", length = 64)
    private String dsReferenceNumber;

    @Column(name = "acs_url", length = 512)
    private String acsUrl;

    @Column(name = "term_url", length = 512)
    private String termUrl;

    @Column(name = "notification_url", length = 512)
    private String notificationUrl;

    @Column(columnDefinition = "TEXT")
    private String creq;

    @Column(columnDefinition = "TEXT")
    private String cres;

    @Column(name = "authentication_value", length = 128)
    private String authenticationValue;

    @Column(length = 2)
    private String eci;

    @Column(name = "ds_trans_id", length = 64)
    private String dsTransId;

    @Column(name = "acs_trans_id", length = 64)
    private String acsTransId;

    @Column(name = "sdk_trans_id", length = 64)
    private String sdkTransId;

    @Column(name = "three_ds_method_data", columnDefinition = "TEXT")
    private String threeDsMethodData;

    @Column(name = "three_ds_method_url", length = 512)
    private String threeDsMethodUrl;

    @Column(name = "challenge_request", columnDefinition = "TEXT")
    private String challengeRequest;

    @Column(name = "challenge_response", columnDefinition = "TEXT")
    private String challengeResponse;

    @Column(name = "device_info", columnDefinition = "TEXT")
    private String deviceInfo;

    @Column(name = "browser_accept_header", length = 512)
    private String browserAcceptHeader;

    @Column(name = "browser_ip", length = 45)
    private String browserIp;

    @Column(name = "browser_language", length = 10)
    private String browserLanguage;

    @Column(name = "browser_color_depth", length = 10)
    private String browserColorDepth;

    @Column(name = "browser_screen_height")
    private Integer browserScreenHeight;

    @Column(name = "browser_screen_width")
    private Integer browserScreenWidth;

    @Column(name = "browser_timezone_offset")
    private Integer browserTimezoneOffset;

    @Column(name = "browser_user_agent", columnDefinition = "TEXT")
    private String browserUserAgent;

    @Column(name = "error_description", columnDefinition = "TEXT")
    private String errorDescription;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
        if (status == null) status = Status.CREATED;
        if (threeDsVersion == null) threeDsVersion = "2.2.0";
        if (authenticationType == null) authenticationType = "PAYMENT";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum Status {
        CREATED, AUTH_REQ_SENT, AUTH_REQ_RECEIVED, CHALLENGE_SENT,
        CHALLENGE_RECEIVED, COMPLETED, ERROR, TIMEOUT
    }
}
