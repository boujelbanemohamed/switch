package com.switchplatform.platform.model;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "routing_rules")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoutingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer priority = 100;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_participant_id")
    private Participant sourceParticipant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_participant_id", nullable = false)
    private Participant destinationParticipant;

    @JsonRawValue
    @JdbcTypeCode(SqlTypes.JSON)
    @JsonDeserialize(using = JsonNodeToStringDeserializer.class)
    @Column(name = "condition_expression", nullable = false, columnDefinition = "JSONB")
    private String conditionExpression;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Protocol protocol = Protocol.BOTH;

    @Column(name = "message_type", length = 10)
    private String messageType;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RuleStatus status = RuleStatus.ACTIVE;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public enum Protocol {
        ISO8583, ISO20022, BOTH
    }

    public enum RuleStatus {
        ACTIVE, INACTIVE
    }

    public static class JsonNodeToStringDeserializer extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser p, DeserializationContext ctx) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            return node != null ? node.toString() : null;
        }
    }
}
