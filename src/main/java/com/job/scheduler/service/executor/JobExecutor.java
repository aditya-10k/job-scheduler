package com.job.scheduler.service.executor;

import com.job.scheduler.model.Job;
import com.job.scheduler.model.JobType;

public interface JobExecutor {

    JobType supports();

    void execute(Job job);
}
