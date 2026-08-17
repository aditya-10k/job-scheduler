package com.job.scheduler.service;

import com.job.scheduler.config.WorkerProperties;
import com.job.scheduler.model.Job;
import com.job.scheduler.model.JobStatus;
import com.job.scheduler.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LeaseRecoveryService {

    private final JobRepository jobRepository;
    private final JobStateService jobStateService;
    private final WorkerProperties workerProperties;

    @Scheduled(fixedDelayString = "${app.worker.lease-recovery-interval-ms:5000}")
    public void recoverExpiredLeases() {
        if (!workerProperties.isEnabled()) {
            return;
        }

        Instant now = Instant.now();
        Instant cutoff = now.minus(workerProperties.getLeaseDuration());
        List<Job> staleJobs = jobRepository.findByJobStatusAndLockedAtLessThanEqual(JobStatus.RUNNING, cutoff);

        for (Job staleJob : staleJobs) {
            log.warn("Recovering stale job {} previously held by {}", staleJob.getId(), staleJob.getWorkerId());
            jobStateService.recoverExpiredLease(staleJob.getId(), now);
        }
    }
}
