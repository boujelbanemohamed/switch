package com.switch.platform.router;

import com.switch.platform.model.Participant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingResult {
    private boolean matched;
    private UUID ruleId;
    private String ruleName;
    private Participant sourceParticipant;
    private Participant destinationParticipant;
}
