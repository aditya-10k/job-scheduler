package com.job.scheduler.queue;

import com.job.scheduler.model.Job;
import com.job.scheduler.model.JobStatus;
import com.job.scheduler.repository.JobRepository;
import com.job.scheduler.config.SchedulerProperties;
import com.job.scheduler.service.SchedulerDispatchService;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobScheduler {

    private final JobRepository jobRepository;
    private final SchedulerDispatchService schedulerDispatchService;
    private final SchedulerProperties schedulerProperties;

    @Scheduled(fixedDelayString = "${app.scheduler.poll-interval-ms:1000}")
    public void scheduleJobs() {
        if (!schedulerProperties.isEnabled()) {
            return;
        }

        Instant now = Instant.now();

        List<Job> jobs = jobRepository.findByJobStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                JobStatus.PENDING,
                now,
                PageRequest.of(0, schedulerProperties.getBatchSize())
        );

        for (Job job : jobs) {
            if (schedulerDispatchService.claimAndStage(job.getId(), now)) {
                log.info("Claimed job {} and staged outbox event", job.getId());
            }
        }
    }
}
