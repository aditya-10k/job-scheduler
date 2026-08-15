package com.job.scheduler.messaging;

import com.job.scheduler.dto.JobExecutionMessage;

public interface JobExecutionPublisher {

    void publish(JobExecutionMessage message);
}
