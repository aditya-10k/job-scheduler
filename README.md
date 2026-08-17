(ps : the documentation is done by ai , coding is done by yours truely(lil help frm gpt ;)) )
# Distributed Job Scheduler (Version 2)

A production-style, fault-tolerant **Distributed Job Scheduler** built using Java, Spring Boot 3.x, PostgreSQL, Apache Kafka, and Docker.

This implementation demonstrates major distributed-system patterns (Atomic Claiming, Transactional Outbox Pattern, Worker Lease Recovery, and Observability) rather than simple CRUD operations.

---

## System Architecture

The application runs in a multi-role containerized deployment:
- **API**: Exposes REST endpoints to create and retrieve jobs. (Schedulers and workers are disabled on this node).
- **Schedulers**: Two instances (`scheduler-1` and `scheduler-2`) run concurrently. They check PostgreSQL for due pending jobs and atomically claim them.
- **Workers**: Two instances (`worker-1` and `worker-2`) run concurrently in a shared Kafka consumer group. They consume execution events and execute them using the Strategy pattern.

```
                      ┌──────────────┐
                      │    Client    │
                      └──────┬───────┘
                             │
                             ▼
                      ┌──────────────┐
                      │  REST API    │
                      └──────┬───────┘
                             │
                             ▼
                      ┌──────────────┐
                      │ PostgreSQL   │
                      │  Jobs Table  │
                      │ Outbox Table │
                      └──────┬───────┘
                             │
                      due pending jobs
                             │
                 ┌───────────┴───────────┐
                 │                       │
                 ▼                       ▼
           Scheduler 1             Scheduler 2
                 │                       │
                 └───────────┬───────────┘
                             │
                      atomic claiming
                             │
                             ▼
                      ┌──────────────┐
                      │   Outbox     │
                      │  Publisher   │
                      └──────┬───────┘
                             │
                      publishes event
                             │
                             ▼
                      ┌──────────────┐
                      │ Kafka Topic  │
                      │ job-execution│
                      └──────┬───────┘
                             │
                  ┌──────────┴──────────┐
                  ▼                     ▼
             Worker 1               Worker 2
                  │                     │
                  └──────────┬──────────┘
                             │
                       Job Execution
                             │
                             ▼
                        PostgreSQL
```

---

## Core Features & Patterns

### 1. Atomic Job Claiming
Schedulers query for due pending jobs (`scheduledAt <= now`). To prevent race conditions where multiple schedulers claim the same job, we use an atomic database operation:
```sql
UPDATE jobs
SET job_status = 'RUNNING', updated_at = :now
WHERE id = :jobId AND job_status = 'PENDING' AND scheduled_at <= :now;
```
If the affected row count is `1`, the scheduler successfully claimed the job and proceeds to execute the outbox operation. If `0`, another scheduler already claimed it, and the current scheduler skips it.

### 2. Transactional Outbox Pattern
To prevent dual-write consistency issues between PostgreSQL and Kafka, the scheduler does *not* write directly to Kafka. Instead, when claiming a job, it:
1. Updates the job state to `RUNNING`.
2. Inserts an event record into the `outbox_events` table.

Both operations are committed in a single, atomic PostgreSQL transaction. A separate, asynchronous `OutboxPublisher` polls the `outbox_events` table (using `FOR UPDATE SKIP LOCKED` to support multiple publisher instances safely), publishes the events to Kafka, and marks them as published.

### 3. Worker Lease & Failure Recovery
When a worker receives a job execution request via Kafka, it must acquire a lease in the database to prevent concurrent processing of redeliveries:
```sql
UPDATE jobs
SET worker_id = :workerId, locked_at = :lockedAt, updated_at = :lockedAt
WHERE id = :jobId
  AND job_status = 'RUNNING'
  AND (worker_id IS NULL OR worker_id = :workerId OR locked_at < :leaseCutoff);
```
If a worker crashes during execution, the job stays stuck in `RUNNING`. A background `LeaseRecoveryService` runs periodically, finds jobs with expired leases (e.g. older than 30s), transitions them back to `FAILED` with a lease expiration reason, and triggers the retry policy.

### 4. Exponential Backoff Retries
If a job execution fails or recovers from an expired lease, the system checks if `retries < maxRetries`. If so, it increments the retry count and schedules it for future execution using an exponential backoff formula:
$$\text{delay} = \text{initialDelay} \times \text{multiplier}^{\text{attempt} - 1}$$
For example, with a 1s initial delay and a 2.0 multiplier, retries are scheduled at +1s, +2s, +4s, etc.

---

## How to Run the Project

### Prerequisites
- Docker & Docker Compose
- Java 17+ (if running tests locally)

### 1. Build and Start the Cluster
To build the application JAR and launch PostgreSQL, Kafka, the API, two schedulers, and two workers:
```bash
docker compose up --build
```

### 2. View running containers
```bash
docker compose ps
```

---

## How to Try and Test the System

### 1. Submit a Job
Submit a scheduled job request to the REST API on port `8080`.

**Create an Email Job (Scheduled to execute immediately):**
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "EMAIL",
    "payload": {
      "to": "user@example.com",
      "subject": "Welcome",
      "body": "Hello World!"
    },
    "scheduledAt": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"
  }'
```

**Create a Report Job (Scheduled for 30 seconds in the future):**
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "jobType": "REPORT",
    "payload": {
      "reportId": 456,
      "format": "PDF"
    },
    "scheduledAt": "'$(date -u -v+30S +"%Y-%m-%dT%H:%M:%SZ" 2>/dev/null || date -u -d "+30 seconds" +"%Y-%m-%dT%H:%M:%SZ")'"
  }'
```

### 2. Monitor Logs
Watch how the schedulers atomically claim the jobs and how workers consume them:
```bash
docker compose logs -f api scheduler-1 scheduler-2 worker-1 worker-2
```
Look for lines in the logs showing:
- Schedulers discovering due jobs and claiming them.
- Outbox publishing the event.
- Kafka message delivery.
- Workers acquiring the lease and executing the job (simulating side effects).

### 3. Check Metrics & Observability
Spring Boot Actuator collects and exposes Micrometer metrics.

Expose the list of available metrics:
```bash
curl http://localhost:8080/actuator/metrics
```

Get counts of completed/failed/retried jobs:
```bash
curl http://localhost:8080/actuator/metrics/scheduler.jobs.completed
curl http://localhost:8080/actuator/metrics/scheduler.jobs.failed
curl http://localhost:8080/actuator/metrics/scheduler.jobs.retried
```

---

## Documentation Links

For a deeper dive into the system design, patterns, and scenarios, refer to the files in the `docs` directory:
- [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md): Component diagrams, sequence diagrams, and schema details.
- [docs/DISTRIBUTED_SYSTEMS.md](docs/DISTRIBUTED_SYSTEMS.md): A comprehensive analysis of consistency, at-least-once delivery, idempotency, backoffs, and leases.
- [docs/FAILURE_SCENARIOS.md](docs/FAILURE_SCENARIOS.md): Step-by-step trace of behavior during node crashes, network cuts, and database outages.
