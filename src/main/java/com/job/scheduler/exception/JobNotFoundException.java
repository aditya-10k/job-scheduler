package com.job.scheduler.exception;

import java.util.UUID;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(UUID jobId) {
        super("Job not found: " + jobId);
    }
}
