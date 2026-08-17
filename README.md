(ps : the documentation is done by ai , coding is done by yours truely(lil help frm gpt ;)) )

# Distributed Job Scheduler

This repository contains two different versions of a Job Scheduler implementation, demonstrating the evolution from a simple in-memory, single-node design to a production-grade, fault-tolerant distributed system.

## Branch Overview

The project is structured into three branches:

*   **`version1`**: A simple, multi-threaded in-memory Job Scheduler console application. This represents the Low-Level Design (LLD) foundation of scheduling and prioritizing jobs on a single node.
*   **`version2`**: A fully functional, production-style Distributed Job Scheduler built using Java, Spring Boot, PostgreSQL, Kafka, and Docker. It is designed to handle multiple scheduler and worker instances concurrently, ensuring fault tolerance, reliability, and high scalability.

---

## Version 1: Single-Node In-Memory LLD Approach (`version1` branch)

The first version focuses on clean object-oriented architecture and concurrent execution within a single JVM.

### Key Features
*   **Priority-Based Execution**: Jobs are processed based on their assigned priority using a `PriorityBlockingQueue` with a custom comparator.
*   **Concurrent Thread Pool**: Multiple worker threads poll from the queue simultaneously to execute jobs.
*   **Strategy Pattern**: Clean abstraction using `JobExecutor` and `JobExecutorFactory` to decouple job types (e.g., `EMAIL`) from the core scheduling engine.

### Limitations
*   **Transient State**: All jobs reside in memory. If the application crashes, all pending and executing jobs are lost.
*   **No Horizontal Scaling**: It is confined to a single JVM. You cannot run multiple instances to share the workload.
*   **No Fault Recovery**: Failed jobs cannot be recovered or retried with backoff.

---

## Version 2: Production-Style Distributed Implementation (`version2` branch)

The second version addresses all the single-node limitations by introducing persistent state, message brokers, and distributed coordination.

### Key Features

*   **Persistent Source of Truth (PostgreSQL)**: Stores job metadata, lifecycle states, retry counts, and worker lease information. It uses PostgreSQL `JSONB` for flexible, schema-less job payloads.
*   **Atomic Claiming & Racing Prevention**: Guarantees that multiple scheduler instances can run concurrently without double-claiming. Atomic claiming is achieved via a single SQL query:
    ```sql
    UPDATE jobs SET job_status = 'RUNNING' WHERE id = ? AND job_status = 'PENDING';
    ```
*   **Transactional Outbox Pattern**: Decouples database state updates from Kafka publishing. When claiming a job, the scheduler updates the job and inserts an outbox event in the *same* database transaction. A separate `OutboxPublisher` processes these events, ensuring reliable eventual publication to Kafka even if Kafka experiences temporary downtime.
*   **Durable Message Distribution (Kafka)**: Decouples schedulers and workers using Apache Kafka in KRaft mode. Workers consume execution messages in a shared consumer group (`job-workers`), ensuring partition-based load balancing.
*   **Worker Lease & Crash Recovery**: Workers acquire a database lease (`locked_at`, `worker_id`) before execution. If a worker crashes during execution, a background `LeaseRecoveryService` identifies expired leases and requeues the jobs.
*   **Exponential Backoff Retries**: Failed jobs are transitioned back to `PENDING` with a calculated exponential backoff delay (e.g., 1s, 2s, 4s...) as long as retries are remaining.
*   **Observability**: Integrates Spring Boot Actuator to expose metrics (jobs created, completed, failed, retries, and execution durations) to Prometheus.

### Architecture Flow

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

## Detailed Architectural Analysis

To explore the deeper design details, distributed system concepts, and failure mitigation strategies implemented in `version2`, refer to the documentation inside the `docs/` folder in the `version2` branch:
1. **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**: Details components, data models, deployment structure, and sequence flows.
2. **[docs/DISTRIBUTED_SYSTEMS.md](docs/DISTRIBUTED_SYSTEMS.md)**: Explains the theoretical reasoning behind transactions, atomic claiming, outbox pattern, consumer groups, idempotency, and eventual consistency.
3. **[docs/FAILURE_SCENARIOS.md](docs/FAILURE_SCENARIOS.md)**: Run-throughs of what happens when a scheduler crashes, Kafka is offline, a worker dies, or duplicate messages are received.
