package com.job.scheduler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.scheduler")
public class SchedulerProperties {

    private boolean enabled = true;
    private int batchSize = 25;
    private long pollIntervalMs = 1000;
}
