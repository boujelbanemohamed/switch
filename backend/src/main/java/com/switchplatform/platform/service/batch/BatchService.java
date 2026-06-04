package com.switchplatform.platform.service.batch;

import com.switchplatform.platform.event.BatchJobCompletedEvent;
import com.switchplatform.platform.event.BatchJobStartedEvent;
import com.switchplatform.platform.event.EventPublisher;
import com.switchplatform.platform.model.Participant;
import com.switchplatform.platform.model.batch.BatchJob;
import com.switchplatform.platform.model.batch.BatchJob.JobType;
import com.switchplatform.platform.model.batch.BatchJob.Status;
import com.switchplatform.platform.repository.ParticipantRepository;
import com.switchplatform.platform.repository.batch.BatchJobRepository;
import com.switchplatform.platform.service.standin.StandInService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchService {

    private final BatchJobRepository batchJobRepository;
    private final EntityManager entityManager;
    private final EventPublisher eventPublisher;
    private final StandInService standInService;
    private final ParticipantRepository participantRepository;

    @Transactional
    public BatchJob scheduleJob(JobType jobType, String jobName) {
        BatchJob job = BatchJob.builder()
                .jobName(jobName)
                .jobType(jobType)
                .status(Status.SCHEDULED)
                .scheduledAt(OffsetDateTime.now())
                .createdAt(OffsetDateTime.now())
                .triggeredBy("MANUAL")
                .build();
        return batchJobRepository.save(job);
    }

    @Transactional
    public BatchJob runJob(UUID jobId) {
        BatchJob job = batchJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.setStatus(Status.RUNNING);
        job.setStartedAt(OffsetDateTime.now());
        return batchJobRepository.save(job);
    }

    @Transactional
    public BatchJob completeJob(UUID jobId, int processed, int failed, String summary) {
        BatchJob job = batchJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.setStatus(Status.COMPLETED);
        job.setCompletedAt(OffsetDateTime.now());
        job.setRecordsProcessed(processed);
        job.setRecordsFailed(failed);
        job.setResultSummary(summary);
        return batchJobRepository.save(job);
    }

    @Transactional
    public BatchJob failJob(UUID jobId, String error) {
        BatchJob job = batchJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.setStatus(Status.FAILED);
        job.setCompletedAt(OffsetDateTime.now());
        job.setErrorMessage(error);
        return batchJobRepository.save(job);
    }

    @Scheduled(cron = "0 0 23 * * *")
    @Transactional
    public void executeEOD() {
        log.info("=== Starting EOD batch ===");
        BatchJob job = scheduleJob(JobType.EOD_CLEARING, "EOD-" + LocalDate.now());
        runJob(job.getId());
        eventPublisher.publishBatchJobStarted(new BatchJobStartedEvent(
                job.getId(), job.getJobName(), job.getJobType().name(), OffsetDateTime.now()));
        try {
            Query q = entityManager.createNativeQuery(
                "UPDATE clearing_records SET status = 'PENDING' WHERE clearing_date = :today AND status = 'CLEARED'"
            );
            q.setParameter("today", LocalDate.now());
            int processed = q.executeUpdate();

            String summary = "{\"records_marked\":" + processed + "}";
            completeJob(job.getId(), processed, 0, summary);
            eventPublisher.publishBatchJobCompleted(new BatchJobCompletedEvent(
                    job.getId(), job.getJobName(), job.getJobType().name(), "COMPLETED",
                    processed, 0, OffsetDateTime.now()));
            log.info("EOD completed: {} records marked", processed);
        } catch (Exception e) {
            log.error("EOD failed", e);
            failJob(job.getId(), e.getMessage());
            eventPublisher.publishBatchJobCompleted(new BatchJobCompletedEvent(
                    job.getId(), job.getJobName(), job.getJobType().name(), "FAILED",
                    0, 0, OffsetDateTime.now()));
        }
    }

    @Scheduled(cron = "0 0 6 * * *")
    @Transactional
    public void executeBOD() {
        log.info("=== Starting BOD batch ===");
        BatchJob job = scheduleJob(JobType.BOD_POSITIONS, "BOD-" + LocalDate.now());
        runJob(job.getId());
        eventPublisher.publishBatchJobStarted(new BatchJobStartedEvent(
                job.getId(), job.getJobName(), job.getJobType().name(), OffsetDateTime.now()));
        try {
            Query q = entityManager.createNativeQuery(
                "UPDATE multilateral_positions SET settlement_status = 'SETTLED' WHERE settlement_status = 'PENDING'"
            );
            int processed = q.executeUpdate();

            String summary = "{\"positions_settled\":" + processed + "}";
            completeJob(job.getId(), processed, 0, summary);
            eventPublisher.publishBatchJobCompleted(new BatchJobCompletedEvent(
                    job.getId(), job.getJobName(), job.getJobType().name(), "COMPLETED",
                    processed, 0, OffsetDateTime.now()));
            log.info("BOD completed: {} positions settled", processed);
        } catch (Exception e) {
            log.error("BOD failed", e);
            failJob(job.getId(), e.getMessage());
            eventPublisher.publishBatchJobCompleted(new BatchJobCompletedEvent(
                    job.getId(), job.getJobName(), job.getJobType().name(), "FAILED",
                    0, 0, OffsetDateTime.now()));
        }
    }

    public List<BatchJob> getJobHistory(JobType jobType) {
        return batchJobRepository.findByStatusOrderByScheduledAtDesc(Status.COMPLETED);
    }

    public List<BatchJob> getRecentJobs() {
        return batchJobRepository.findByStatusOrderByScheduledAtDesc(Status.SCHEDULED);
    }

    @Scheduled(cron = "0 30 6 * * *")
    @Transactional
    public void reconcileStandInAuthorizations() {
        log.info("=== Starting stand-in authorization reconciliation ===");
        List<Participant> issuers = participantRepository.findByTypeAndStatus(
                Participant.ParticipantType.ISSUER, Participant.ParticipantStatus.ACTIVE);
        for (Participant issuer : issuers) {
            try {
                standInService.reconcilePending(issuer.getId());
                log.info("Reconciled stand-in authorizations for issuer {}", issuer.getCode());
            } catch (Exception e) {
                log.error("Failed to reconcile stand-in for issuer {}: {}", issuer.getCode(), e.getMessage());
            }
        }
    }
}
