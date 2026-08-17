# Failure Scenarios

## Scheduler Crash

Scenario:

```text
Scheduler claims job
-> crashes
```

Behavior:

- if the crash happens before the transaction commits, the claim and outbox insert both roll back
- if the transaction commits, the outbox row remains durable in PostgreSQL
- another scheduler or publisher instance can continue from that persisted state

## Kafka Unavailable

Scenario:

```text
Job claimed
-> Kafka unavailable
```

Behavior:

- the scheduler has already committed the outbox row
- `OutboxPublisher` keeps the row unpublished and increments publication attempts
- once Kafka returns, the publisher retries from the same durable outbox record

## Worker Crash

Scenario:

```text
Worker receives job
-> crashes
```

Behavior:

- the job remains `RUNNING` with `worker_id` and `locked_at`
- when `locked_at` becomes older than the configured lease timeout, `LeaseRecoveryService` requeues the job
- if retries remain, the job returns to `PENDING` with backoff

## Duplicate Kafka Message

Scenario:

```text
Same job delivered twice
```

Behavior:

- a completed job is ignored on redelivery
- a running job requires a lease before execution
- if another worker already holds the lease, the duplicate delivery is skipped

This mitigates duplicates, but interview-wise it is important to say that external side effects may still need their own idempotency key.

## Concurrent Schedulers

Scenario:

```text
Scheduler 1 + Scheduler 2
-> same job
```

Behavior:

- both can read the same due job
- only one `UPDATE ... WHERE job_status = 'PENDING'` succeeds
- only the successful claimant writes the outbox row

## Database Failure

Scenario:

```text
DB unavailable
```

Behavior:

- API requests to create or fetch jobs fail fast
- schedulers cannot claim work
- workers cannot validate or complete jobs
- because PostgreSQL is the source of truth, the system prefers stopping over guessing

That tradeoff is intentional: correctness is more important than pretending work succeeded without durable state.
