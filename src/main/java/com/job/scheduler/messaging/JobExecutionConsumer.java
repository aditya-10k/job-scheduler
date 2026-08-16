package com.job.scheduler.messaging;

import com.job.scheduler.dto.JobExecutionMessage;
import com.job.scheduler.service.JobWorkerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobExecutionConsumer {

    private final JobWorkerService jobWorkerService;

    @KafkaListener(
            topics = "${app.kafka.topic}",
            groupId = "${app.kafka.consumer-group}",
            autoStartup = "${app.worker.enabled:true}"
    )
    public void consume(JobExecutionMessage message) {
        log.info("Received execution message for job {}", message.jobId());
        jobWorkerService.process(message);
    }
}
