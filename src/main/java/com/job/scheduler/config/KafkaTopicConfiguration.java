package com.job.scheduler.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@RequiredArgsConstructor
public class KafkaTopicConfiguration {

    private final KafkaTopicProperties kafkaTopicProperties;

    @Bean
    public NewTopic jobExecutionTopic() {
        return TopicBuilder.name(kafkaTopicProperties.getTopic())
                .partitions(4)
                .replicas(1)
                .build();
    }
}
