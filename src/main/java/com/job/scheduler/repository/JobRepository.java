package com.job.scheduler.repository;

import com.job.scheduler.model.Job;
import com.job.scheduler.model.JobStatus;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByJobStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
            JobStatus jobStatus,
            Instant scheduledAt,
            Pageable pageable
    );

    List<Job> findByJobStatusAndLockedAtLessThanEqual(JobStatus jobStatus, Instant lockedAt);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE jobs
            SET job_status = 'RUNNING',
                worker_id = NULL,
                locked_at = NULL,
                updated_at = :now,
                last_error = NULL
            WHERE id = :jobId
              AND job_status = 'PENDING'
              AND scheduled_at <= :now
            """, nativeQuery = true)
    int claimDueJob(
            @Param("jobId") UUID jobId,
            @Param("now") Instant now
    );

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE jobs
            SET worker_id = :workerId,
                locked_at = :lockedAt,
                updated_at = :lockedAt
            WHERE id = :jobId
              AND job_status = 'RUNNING'
              AND (worker_id IS NULL OR worker_id = :workerId OR locked_at < :leaseCutoff)
            """, nativeQuery = true)
    int acquireLease(
            @Param("jobId") UUID jobId,
            @Param("workerId") String workerId,
            @Param("lockedAt") Instant lockedAt,
            @Param("leaseCutoff") Instant leaseCutoff
    );
}
