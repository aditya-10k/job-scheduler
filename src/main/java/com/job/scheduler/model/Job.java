package com.job.scheduler.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "jobs",
        indexes = {
                @Index(name = "idx_jobs_status_schedule", columnList = "job_status, scheduled_at"),
                @Index(name = "idx_jobs_status_locked_at", columnList = "job_status, locked_at")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobStatus jobStatus;

    @Column(nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    @Builder.Default
    private Map<String, Object> payload = new LinkedHashMap<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant scheduledAt;

    @Builder.Default
    @Column(nullable = false)
    private int retries = 0;

    @Builder.Default
    @Column(nullable = false)
    private int maxRetries = 3;

    private Instant completedAt;

    @Column(length = 128)
    private String workerId;

    private Instant lockedAt;

    @Column(length = 1024)
    private String lastError;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = createdAt == null ? now : createdAt;
        updatedAt = updatedAt == null ? now : updatedAt;
        jobStatus = jobStatus == null ? JobStatus.PENDING : jobStatus;
        payload = payload == null ? new LinkedHashMap<>() : payload;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean hasRetriesRemaining() {
        return retries < maxRetries;
    }

    public void clearLease() {
        workerId = null;
        lockedAt = null;
    }
}
