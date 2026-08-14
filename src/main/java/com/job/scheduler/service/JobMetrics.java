package com.job.scheduler.service;

import com.job.scheduler.model.JobType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class JobMetrics {

    private final MeterRegistry meterRegistry;

    public JobMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordJobCreated(JobType jobType) {
        counter("scheduler.jobs.created", jobType).increment();
    }

    public void recordJobCompleted(JobType jobType) {
        counter("scheduler.jobs.completed", jobType).increment();
    }

    public void recordJobFailed(JobType jobType) {
        counter("scheduler.jobs.failed", jobType).increment();
    }

    public void recordJobRetried(JobType jobType) {
        counter("scheduler.jobs.retried", jobType).increment();
    }

    public void recordSchedulerClaim(boolean success) {
        Counter.builder(success ? "scheduler.claims.success" : "scheduler.claims.failed")
                .register(meterRegistry)
                .increment();
    }

    public void recordExecutionDuration(JobType jobType, Duration duration) {
        Timer.builder("scheduler.jobs.execution.duration")
                .tag("jobType", jobType.name())
                .register(meterRegistry)
                .record(duration);
    }

    private Counter counter(String name, JobType jobType) {
        return Counter.builder(name)
                .tag("jobType", jobType.name())
                .register(meterRegistry);
    }
}
