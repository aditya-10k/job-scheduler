package com.job.scheduler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.retry")
public class RetryProperties {

    private int defaultMaxRetries = 3;
    private Duration initialDelay = Duration.ofSeconds(1);
    private double multiplier = 2.0;
    private Duration maxDelay = Duration.ofSeconds(30);
}
