package com.job.scheduler.service;

import com.job.scheduler.dto.JobExecutionMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JobWorkerService {
    public void process(JobExecutionMessage message) {
        log.info("Skeleton processing job: {}", message.jobId());
    }
}
