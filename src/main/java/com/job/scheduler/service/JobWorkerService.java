package com.job.scheduler.service;

import com.job.scheduler.config.WorkerProperties;
import com.job.scheduler.dto.JobExecutionMessage;
import com.job.scheduler.model.Job;
import com.job.scheduler.model.JobStatus;
import com.job.scheduler.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobWorkerService {

    private final JobRepository jobRepository;
    private final JobStateService jobStateService;
    private final JobExecutorRegistry jobExecutorRegistry;
    private final WorkerProperties workerProperties;
    private final JobMetrics jobMetrics;

    public void process(JobExecutionMessage message) {
        Optional<Job> existingJob = jobRepository.findById(message.jobId());
        if (existingJob.isEmpty()) {
            log.warn("Dropping execution message for missing job {}", message.jobId());
            return;
        }

        Job snapshot = existingJob.get();
        if (snapshot.getJobStatus() == JobStatus.COMPLETED) {
            log.info("Ignoring duplicate delivery for completed job {}", snapshot.getId());
            return;
        }

        if (snapshot.getJobStatus() != JobStatus.RUNNING) {
            log.info("Ignoring delivery for job {} because status is {}", snapshot.getId(), snapshot.getJobStatus());
            return;
        }

        Instant now = Instant.now();
        int acquired = jobRepository.acquireLease(
                snapshot.getId(),
                workerProperties.getInstanceId(),
                now,
                now.minus(workerProperties.getLeaseDuration())
        );

        if (acquired == 0) {
            log.info("Worker {} did not acquire lease for job {}", workerProperties.getInstanceId(), snapshot.getId());
            return;
        }

        Job job = jobRepository.findById(snapshot.getId()).orElseThrow();
        Instant startedAt = Instant.now();

        try {
            jobExecutorRegistry.getExecutor(job.getJobType()).execute(job);
            jobStateService.markCompleted(job.getId(), Instant.now());
        } catch (Exception exception) {
            log.warn("Job {} failed on worker {}: {}", job.getId(), workerProperties.getInstanceId(), exception.getMessage());
            jobStateService.handleExecutionFailure(job.getId(), Instant.now(), exception.getMessage());
        } finally {
            jobMetrics.recordExecutionDuration(job.getJobType(), Duration.between(startedAt, Instant.now()));
        }
    }
}
