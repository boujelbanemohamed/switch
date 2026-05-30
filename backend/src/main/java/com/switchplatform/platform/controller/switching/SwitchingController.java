package com.switchplatform.platform.controller.switching;

import com.switchplatform.platform.model.DLQRecord;
import com.switchplatform.platform.service.MessageHandlerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/switch/mq")
@RequiredArgsConstructor
@Slf4j
public class SwitchingController {

    private final MessageHandlerService messageHandlerService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getMqStatus() {
        return ResponseEntity.ok(messageHandlerService.getMqStatus());
    }

    @GetMapping("/dlq")
    public ResponseEntity<List<DLQRecord>> getDeadLetterQueue() {
        return ResponseEntity.ok(messageHandlerService.getDeadLetterQueue());
    }

    @GetMapping("/dlq/count")
    public ResponseEntity<Map<String, Integer>> getDlqCount() {
        return ResponseEntity.ok(Map.of("count", messageHandlerService.getDlqCount()));
    }

    @PostMapping("/dlq/{dlqId}/retry")
    public ResponseEntity<Map<String, Object>> retryDlqMessage(@PathVariable UUID dlqId) {
        boolean success = messageHandlerService.retryDlqMessage(dlqId);
        if (success) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "DLQ message retried successfully"
            ));
        }
        return ResponseEntity.ok(Map.of(
                "success", false,
                "message", "DLQ message not found or retry failed"
        ));
    }
}
