package com.switchplatform.platform.model.admin;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "live_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LiveConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false, columnDefinition = "TEXT")
    private String configValue;

    @Column(length = 500)
    private String description;

    @Column(name = "data_type", nullable = false, length = 20)
    @Builder.Default
    private String dataType = "STRING";

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String category = "GENERAL";

    @Column(nullable = false)
    @Builder.Default
    private Boolean mutable = true;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
