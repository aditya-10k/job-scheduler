package com.job.scheduler.service.executor;

import com.job.scheduler.exception.JobExecutionException;
import com.job.scheduler.model.Job;
import com.job.scheduler.model.JobType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ReportJobExecutor implements JobExecutor {

    @Override
    public JobType supports() {
        return JobType.REPORT;
    }

    @Override
    public void execute(Job job) {
        maybeFail(job);
        log.info("Simulated REPORT job {} generated report {}", job.getId(), job.getPayload().get("reportId"));
    }

    private void maybeFail(Job job) {
        Object shouldFail = job.getPayload().get("shouldFail");
        if (Boolean.TRUE.equals(shouldFail)) {
            throw new JobExecutionException("Simulated REPORT generation failure");
        }
    }
}
