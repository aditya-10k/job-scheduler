package com.job.scheduler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.kafka")
public class KafkaTopicProperties {

    private String topic = "job-execution";
    private String consumerGroup = "job-workers";
}
