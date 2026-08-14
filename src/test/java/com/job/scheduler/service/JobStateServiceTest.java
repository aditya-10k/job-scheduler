package com.job.scheduler.service;

import com.job.scheduler.exception.InvalidJobStateException;
import com.job.scheduler.model.Job;
import com.job.scheduler.model.JobStatus;
import com.job.scheduler.model.JobType;
import com.job.scheduler.repository.JobRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobStateServiceTest {

    @Mock
    private JobRepository jobRepository;

    @Mock
    private BackoffCalculator backoffCalculator;

    @Mock
    private JobMetrics jobMetrics;

    @InjectMocks
    private JobStateService jobStateService;

    @Test
    void markCompletedMovesRunningJobToCompleted() {
        UUID jobId = UUID.randomUUID();
        Instant completionTime = Instant.parse("2026-08-17T10:15:30Z");
        Job job = runningJob(jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        Job result = jobStateService.markCompleted(jobId, completionTime);

        assertThat(result.getJobStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(result.getCompletedAt()).isEqualTo(completionTime);
        assertThat(result.getWorkerId()).isNull();
        assertThat(result.getLockedAt()).isNull();
        verify(jobMetrics).recordJobCompleted(JobType.EMAIL);
    }

    @Test
    void handleExecutionFailureRequeuesJobWithBackoffWhenRetriesRemain() {
        UUID jobId = UUID.randomUUID();
        Instant failureTime = Instant.parse("2026-08-17T10:15:30Z");
        Job job = runningJob(jobId);

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(backoffCalculator.delayForRetry(1)).thenReturn(Duration.ofSeconds(1));

        Job result = jobStateService.handleExecutionFailure(jobId, failureTime, "temporary error");

        assertThat(result.getJobStatus()).isEqualTo(JobStatus.PENDING);
        assertThat(result.getRetries()).isEqualTo(1);
        assertThat(result.getScheduledAt()).isEqualTo(failureTime.plusSeconds(1));
        assertThat(result.getLastError()).isEqualTo("temporary error");
        verify(jobMetrics).recordJobFailed(JobType.EMAIL);
        verify(jobMetrics).recordJobRetried(JobType.EMAIL);
    }

    @Test
    void rejectsInvalidTransitions() {
        UUID jobId = UUID.randomUUID();
        Job job = Job.builder()
                .id(jobId)
                .jobType(JobType.EMAIL)
                .jobStatus(JobStatus.COMPLETED)
                .payload(Map.of("to", "user@example.com"))
                .scheduledAt(Instant.now())
                .build();

        when(jobRepository.findById(jobId)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobStateService.handleExecutionFailure(jobId, Instant.now(), "late failure"))
                .isInstanceOf(InvalidJobStateException.class)
                .hasMessageContaining("COMPLETED -> FAILED");
    }

    private Job runningJob(UUID jobId) {
        return Job.builder()
                .id(jobId)
                .jobType(JobType.EMAIL)
                .jobStatus(JobStatus.RUNNING)
                .payload(Map.of("to", "user@example.com"))
                .scheduledAt(Instant.parse("2026-08-17T10:00:00Z"))
                .workerId("worker-1")
                .lockedAt(Instant.parse("2026-08-17T10:15:00Z"))
                .maxRetries(3)
                .retries(0)
                .build();
    }
}
