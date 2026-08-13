package com.job.scheduler.service;

import com.job.scheduler.dto.JobResponse;
import com.job.scheduler.model.Job;
import org.springframework.stereotype.Component;

@Component
public class JobMapper {

    public JobResponse toResponse(Job job) {
        return new JobResponse(
                job.getId(),
                job.getJobType(),
                job.getJobStatus(),
                job.getPayload(),
                job.getCreatedAt(),
                job.getScheduledAt(),
                job.getRetries(),
                job.getMaxRetries(),
                job.getCompletedAt(),
                job.getWorkerId(),
                job.getLockedAt(),
                job.getLastError()
        );
    }
}
