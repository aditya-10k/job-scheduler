package com.job.scheduler.dto;

import com.job.scheduler.model.JobType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

public record JobRequest(
        @NotNull(message = "jobType is required")
        JobType jobType,
        @NotEmpty(message = "payload is required")
        Map<String , Object> payload ,
        @NotNull(message = "scheduledAt is required")
        Instant scheduledAt
) {
}
