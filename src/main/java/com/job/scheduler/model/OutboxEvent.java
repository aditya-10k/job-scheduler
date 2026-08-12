package com.job.scheduler.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "outbox_events",
        indexes = {
                @Index(name = "idx_outbox_published_created", columnList = "published, created_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;

    private Instant publishedAt;

    @Column(nullable = false)
    @Builder.Default
    private int publishAttempts = 0;

    @Column(length = 1024)
    private String lastError;

    @PrePersist
    protected void onCreate() {
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    public void markPublished(Instant timestamp) {
        published = true;
        publishedAt = timestamp;
        lastError = null;
    }

    public void recordPublishFailure(String error) {
        publishAttempts++;
        lastError = error == null ? "Unknown publication error" : error.substring(0, Math.min(error.length(), 1024));
    }
}
