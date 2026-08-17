package com.job.scheduler.service;

import com.job.scheduler.model.JobType;
import com.job.scheduler.service.executor.JobExecutor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class JobExecutorRegistry {

    private final Map<JobType, JobExecutor> executors = new EnumMap<>(JobType.class);

    public JobExecutorRegistry(List<JobExecutor> executors) {
        for (JobExecutor executor : executors) {
            this.executors.put(executor.supports(), executor);
        }
    }

    public JobExecutor getExecutor(JobType jobType) {
        JobExecutor executor = executors.get(jobType);
        if (executor == null) {
            throw new IllegalArgumentException("No executor registered for job type " + jobType);
        }
        return executor;
    }
}
