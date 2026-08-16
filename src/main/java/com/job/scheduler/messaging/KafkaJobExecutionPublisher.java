package com.job.scheduler.messaging;

import com.job.scheduler.config.KafkaTopicProperties;
import com.job.scheduler.dto.JobExecutionMessage;
import com.job.scheduler.exception.JobExecutionException;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class KafkaJobExecutionPublisher implements JobExecutionPublisher {

    private final KafkaTemplate<String, JobExecutionMessage> kafkaTemplate;
    private final KafkaTopicProperties kafkaTopicProperties;

    @Override
    public void publish(JobExecutionMessage message) {
        try {
            kafkaTemplate.send(kafkaTopicProperties.getTopic(), message.jobId().toString(), message)
                    .get(10, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new JobExecutionException("Failed to publish job " + message.jobId() + " to Kafka", exception);
        }
    }
}
