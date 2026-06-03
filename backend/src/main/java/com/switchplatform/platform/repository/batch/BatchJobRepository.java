package com.switchplatform.platform.repository.batch;

import com.switchplatform.platform.model.batch.BatchJob;
import com.switchplatform.platform.model.batch.BatchJob.JobType;
import com.switchplatform.platform.model.batch.BatchJob.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BatchJobRepository extends JpaRepository<BatchJob, UUID> {

    List<BatchJob> findByJobTypeAndStatusOrderByScheduledAtDesc(JobType jobType, Status status);

    Optional<BatchJob> findTopByJobTypeAndStatusOrderByScheduledAtDesc(JobType jobType, Status status);

    Optional<BatchJob> findTopByJobTypeOrderByScheduledAtDesc(JobType jobType);

    @Query("SELECT bj FROM BatchJob bj WHERE bj.status = 'SCHEDULED' AND bj.scheduledAt <= CURRENT_TIMESTAMP ORDER BY bj.scheduledAt")
    List<BatchJob> findOverdueJobs();

    List<BatchJob> findByStatusOrderByScheduledAtDesc(Status status);
}
