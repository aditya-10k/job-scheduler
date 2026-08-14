package com.job.scheduler.service;

import com.job.scheduler.model.Job;
import com.job.scheduler.model.JobStatus;
import com.job.scheduler.repository.JobRepository;
import com.job.scheduler.exception.InvalidJobStateException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobStateService {

    private static final Map<JobStatus, Set<JobStatus>> ALLOWED_TRANSITIONS =
            Map.of(
                    JobStatus.PENDING, Set.of(JobStatus.RUNNING),
                    JobStatus.RUNNING, Set.of(
                            JobStatus.COMPLETED,
                            JobStatus.FAILED
                    ),
                    JobStatus.FAILED, Set.of(JobStatus.PENDING),
                    JobStatus.COMPLETED, Set.of()
    );

    private final JobRepository jobRepository;
    private final BackoffCalculator backoffCalculator;
    private final JobMetrics jobMetrics;

    @Transactional
    public Job markRunning(UUID jobId) {
        Job job = getJob(jobId);

        validateTransition(job.getJobStatus(), JobStatus.RUNNING);

        job.setJobStatus(JobStatus.RUNNING);

        return job;
    }

    @Transactional
    public Job markCompleted(UUID jobId, Instant completedAt) {
        Job job = getJob(jobId);

        validateTransition(job.getJobStatus(), JobStatus.COMPLETED);

        job.setJobStatus(JobStatus.COMPLETED);
        job.setCompletedAt(completedAt);
        job.setLastError(null);
        job.clearLease();
        jobMetrics.recordJobCompleted(job.getJobType());

        return job;
    }

    @Transactional
    public Job handleExecutionFailure(UUID jobId, Instant failedAt, String reason) {
        Job job = getJob(jobId);

        validateTransition(job.getJobStatus(), JobStatus.FAILED);
        job.setJobStatus(JobStatus.FAILED);
        job.setLastError(truncate(reason));
        job.setCompletedAt(null);
        job.clearLease();
        jobMetrics.recordJobFailed(job.getJobType());

        if (job.hasRetriesRemaining()) {
            validateTransition(JobStatus.FAILED, JobStatus.PENDING);
            int nextAttempt = job.getRetries() + 1;
            job.setRetries(nextAttempt);
            job.setScheduledAt(failedAt.plus(backoffCalculator.delayForRetry(nextAttempt)));
            job.setJobStatus(JobStatus.PENDING);
            jobMetrics.recordJobRetried(job.getJobType());
        }

        return job;
    }

    @Transactional
    public Job recoverExpiredLease(UUID jobId, Instant now) {
        return handleExecutionFailure(jobId, now, "Worker lease expired before job completion");
    }

    private void validateTransition(
            JobStatus currentStatus,
            JobStatus newStatus) {

        Set<JobStatus> allowedStates =
                ALLOWED_TRANSITIONS.getOrDefault(
                        currentStatus,
                        Set.of()
                );

        if (!allowedStates.contains(newStatus)) {
            throw new InvalidJobStateException(
                    "Invalid job state transition: "
                            + currentStatus
                            + " -> "
                            + newStatus
            );
        }
    }

    private Job getJob(UUID jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new com.job.scheduler.exception.JobNotFoundException(jobId));
    }

    private String truncate(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Job execution failed";
        }
        return reason.length() <= 1024 ? reason : reason.substring(0, 1024);
    }
}
