package com.job.scheduler.service;

import com.job.scheduler.model.Job;
import com.job.scheduler.model.JobStatus;
import com.job.scheduler.repository.JobRepository;
import com.job.scheduler.config.RetryProperties;
import com.job.scheduler.dto.JobRequest;
import com.job.scheduler.dto.JobResponse;
import com.job.scheduler.exception.JobNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobMapper jobMapper;
    private final RetryProperties retryProperties;
    private final JobMetrics jobMetrics;

    public JobResponse addJob(JobRequest jobRequest) {

        Job job = Job.builder()
                .jobType(jobRequest.jobType())
                .jobStatus(JobStatus.PENDING)
                .payload(jobRequest.payload())
                .scheduledAt(jobRequest.scheduledAt())
                .maxRetries(retryProperties.getDefaultMaxRetries())
                .build();

        Job savedJob = jobRepository.save(job);
        jobMetrics.recordJobCreated(savedJob.getJobType());

        return jobMapper.toResponse(savedJob);
    }

    public JobResponse findJob(UUID id) {
        return jobMapper.toResponse(getJob(id));
    }

    public Job getJob(UUID id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
    }
}
