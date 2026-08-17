package com.job.scheduler.service.executor;

import com.job.scheduler.exception.JobExecutionException;
import com.job.scheduler.model.Job;
import com.job.scheduler.model.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailJobExecutor implements JobExecutor {

    @Override
    public JobType supports() {
        return JobType.EMAIL;
    }

    @Override
    public void execute(Job job) {
        maybeFail(job);
        log.info("Simulated EMAIL job {} sent to {}", job.getId(), job.getPayload().get("to"));
    }

    private void maybeFail(Job job) {
        Object failUntilAttempt = job.getPayload().get("failUntilAttempt");
        if (failUntilAttempt instanceof Number number && job.getRetries() < number.intValue()) {
            throw new JobExecutionException("Simulated EMAIL failure before retry threshold");
        }
    }
}
