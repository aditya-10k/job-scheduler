package com.job.scheduler.controller;

import com.job.scheduler.dto.JobRequest;
import com.job.scheduler.dto.JobResponse;
import com.job.scheduler.service.JobService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public ResponseEntity<JobResponse> createJob(
            @Valid @RequestBody JobRequest request) {

        JobResponse response = jobService.addJob(request);

        return ResponseEntity
                .created(URI.create("/api/jobs/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JobResponse> getJob(
            @PathVariable UUID id) {

        JobResponse response = jobService.findJob(id);

        return ResponseEntity.ok(response);
    }
}
