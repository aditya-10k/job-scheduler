package com.job.scheduler.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.worker")
public class WorkerProperties {

    private boolean enabled = true;
    private Duration leaseDuration = Duration.ofSeconds(30);
    private long leaseRecoveryIntervalMs = 5000;
    private String instanceId = "worker";
}
