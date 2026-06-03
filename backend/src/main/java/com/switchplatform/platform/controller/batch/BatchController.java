package com.switchplatform.platform.controller.batch;

import com.switchplatform.platform.model.batch.BatchJob;
import com.switchplatform.platform.model.batch.BatchJob.JobType;
import com.switchplatform.platform.service.batch.BatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/batch")
@RequiredArgsConstructor
public class BatchController {

    private final BatchService batchService;

    @PostMapping("/schedule")
    public ResponseEntity<BatchJob> schedule(@RequestParam JobType jobType,
                                              @RequestParam String jobName) {
        return ResponseEntity.ok(batchService.scheduleJob(jobType, jobName));
    }

    @PostMapping("/{jobId}/run")
    public ResponseEntity<BatchJob> run(@PathVariable UUID jobId) {
        return ResponseEntity.ok(batchService.runJob(jobId));
    }

    @PostMapping("/{jobId}/complete")
    public ResponseEntity<BatchJob> complete(@PathVariable UUID jobId,
                                              @RequestParam int processed,
                                              @RequestParam int failed,
                                              @RequestParam(required = false) String summary) {
        return ResponseEntity.ok(batchService.completeJob(jobId, processed, failed, summary));
    }

    @PostMapping("/{jobId}/fail")
    public ResponseEntity<BatchJob> fail(@PathVariable UUID jobId,
                                          @RequestParam String error) {
        return ResponseEntity.ok(batchService.failJob(jobId, error));
    }

    @PostMapping("/eod")
    public ResponseEntity<Void> triggerEOD() {
        batchService.executeEOD();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/bod")
    public ResponseEntity<Void> triggerBOD() {
        batchService.executeBOD();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history")
    public ResponseEntity<List<BatchJob>> history(@RequestParam(required = false) JobType jobType) {
        if (jobType != null) {
            return ResponseEntity.ok(batchService.getJobHistory(jobType));
        }
        return ResponseEntity.ok(batchService.getRecentJobs());
    }
}
