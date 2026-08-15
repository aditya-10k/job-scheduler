package com.job.scheduler.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.job.scheduler.config.OutboxProperties;
import com.job.scheduler.dto.JobExecutionMessage;
import com.job.scheduler.messaging.JobExecutionPublisher;
import com.job.scheduler.model.OutboxEvent;
import com.job.scheduler.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final JobExecutionPublisher jobExecutionPublisher;
    private final ObjectMapper objectMapper;
    private final OutboxProperties outboxProperties;

    @Transactional
    @Scheduled(fixedDelayString = "${app.outbox.publish-interval-ms:1000}")
    public void publishPending() {
        if (!outboxProperties.isEnabled()) {
            return;
        }

        List<OutboxEvent> events = outboxEventRepository.lockNextBatch(outboxProperties.getBatchSize());
        for (OutboxEvent event : events) {
            try {
                JobExecutionMessage message = objectMapper.readValue(event.getPayload(), JobExecutionMessage.class);
                jobExecutionPublisher.publish(message);
                event.markPublished(Instant.now());
            } catch (Exception exception) {
                log.warn("Outbox publish failed for event {}: {}", event.getId(), exception.getMessage());
                event.recordPublishFailure(exception.getMessage());
            }
        }
    }
}
