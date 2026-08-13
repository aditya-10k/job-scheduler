package com.job.scheduler.dto;

import com.job.scheduler.model.JobStatus;
import com.job.scheduler.model.JobType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record JobResponse(
        UUID id,
        JobType jobType,
        JobStatus jobStatus,
        Map<String, Object> payload,
        Instant createdAt,
        Instant scheduledAt,
        int retries,
        int maxRetries,
        Instant completedAt,
        String workerId,
        Instant lockedAt,
        String lastError
) {
}
