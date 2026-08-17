package com.job.scheduler.service.executor;

import com.job.scheduler.exception.JobExecutionException;
import com.job.scheduler.model.Job;
import com.job.scheduler.model.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationJobExecutor implements JobExecutor {

    @Override
    public JobType supports() {
        return JobType.NOTIFICATION;
    }

    @Override
    public void execute(Job job) {
        maybeFail(job);
        log.info("Simulated NOTIFICATION job {} delivered to {}", job.getId(), job.getPayload().get("recipient"));
    }

    private void maybeFail(Job job) {
        if (Boolean.TRUE.equals(job.getPayload().get("shouldFail"))) {
            throw new JobExecutionException("Simulated NOTIFICATION failure requested by payload");
        }
    }
}
