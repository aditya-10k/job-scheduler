package com.job.scheduler.service;

import com.job.scheduler.config.RetryProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class BackoffCalculator {

    private final RetryProperties retryProperties;

    public Duration delayForRetry(int retryAttempt) {
        double multiplier = Math.pow(retryProperties.getMultiplier(), Math.max(0, retryAttempt - 1));
        long proposedMillis = Math.round(retryProperties.getInitialDelay().toMillis() * multiplier);
        long boundedMillis = Math.min(proposedMillis, retryProperties.getMaxDelay().toMillis());
        return Duration.ofMillis(Math.max(0, boundedMillis));
    }
}
