package com.job.scheduler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.job.scheduler.dto.JobExecutionMessage;
import com.job.scheduler.model.OutboxEvent;
import com.job.scheduler.repository.JobRepository;
import com.job.scheduler.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SchedulerDispatchService {

    private static final String JOB_EXECUTION_EVENT = "JOB_EXECUTION_REQUESTED";

    private final JobRepository jobRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final JobMetrics jobMetrics;

    @Transactional
    public boolean claimAndStage(UUID jobId, Instant now) {
        int updated = jobRepository.claimDueJob(jobId, now);
        if (updated == 0) {
            jobMetrics.recordSchedulerClaim(false);
            return false;
        }

        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateId(jobId)
                .eventType(JOB_EXECUTION_EVENT)
                .payload(serialize(new JobExecutionMessage(jobId)))
                .build());

        jobMetrics.recordSchedulerClaim(true);
        return true;
    }

    private String serialize(JobExecutionMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize outbox payload", exception);
        }
    }
}
