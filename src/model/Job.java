package model;

import java.util.UUID;

public abstract class Job {

    private final UUID id;
    private final int priority;
    private JobStatus status;
    private final JobType jobType;

    protected Job(int priority, JobType jobType) {
        this.id = UUID.randomUUID();
        this.priority = priority;
        this.status = JobStatus.QUEUED;
        this.jobType = jobType;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public int getPriority() {
        return priority;
    }

    public JobType getJobType() {
        return jobType;
    }

    public UUID getId() {
        return id;
    }
}