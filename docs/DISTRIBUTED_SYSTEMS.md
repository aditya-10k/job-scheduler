# Distributed Systems Notes

## Race Conditions

Race conditions happen when two nodes observe the same state and both try to act on it. In this project, multiple schedulers may see the same due job. We avoid duplicate claims with a single atomic `UPDATE ... WHERE job_status = 'PENDING'`.

## Atomic State Transitions

The `JobStateService` owns allowed transitions:

- `PENDING -> RUNNING`
- `RUNNING -> COMPLETED`
- `RUNNING -> FAILED`
- `FAILED -> PENDING`

This prevents controllers or workers from applying arbitrary status updates that would break lifecycle guarantees.

## Kafka Concepts In This Project

- Broker: the Kafka server process. Local Docker Compose runs one broker in KRaft mode.
- Topic: the logical stream of execution events. Here it is `job-execution`.
- Partition: unit of parallelism inside a topic. More partitions allow more workers to consume concurrently.
- Producer: the outbox publisher that sends `JobExecutionMessage`.
- Consumer: worker instances using `@KafkaListener`.
- Consumer group: `job-workers`, which spreads records across worker instances.
- Offset: Kafka’s position marker for each consumed record in a partition.
- Retention: Kafka keeps records for a configured window even after consumption.
- Rebalancing: when workers join or leave, Kafka reassigns partitions.

## At-Least-Once Delivery

This design assumes at-least-once delivery:

1. A message can be delivered.
2. A worker can execute it.
3. The worker can crash before a clean acknowledgement path finishes.
4. Kafka can redeliver the message.

That is why business safety comes from idempotent handling, not from pretending Kafka gives exactly-once business execution.

## Idempotency

This project uses job state as the first idempotency guard:

- if the job is already `COMPLETED`, duplicate deliveries are ignored
- if a lease cannot be acquired, the worker backs off because another worker owns the job

For interview discussion: this is enough for a demo, but real external side effects often need a stronger idempotency key stored with the downstream operation.

## Retry And Exponential Backoff

Retries are stored in PostgreSQL, not in memory. On failure:

1. job goes `RUNNING -> FAILED`
2. if retries remain, it becomes `PENDING` again
3. `scheduled_at` is moved into the future with exponential backoff

This avoids tight retry loops and keeps behavior deterministic across multiple instances.

## Worker Leases And Recovery

If a worker crashes after claiming execution, the job should not stay `RUNNING` forever. The lease fields solve that:

- `worker_id` identifies the owner
- `locked_at` records when the lease was acquired

`LeaseRecoveryService` periodically finds stale `RUNNING` jobs whose lease has expired and requeues them through the same retry logic.

## Transactional Outbox

The outbox pattern solves the classic dual-write problem:

- updating PostgreSQL and publishing to Kafka are two separate systems
- doing them directly in sequence can lose events if one side succeeds and the other fails

Instead, the scheduler transaction:

1. marks the job as `RUNNING`
2. inserts an `outbox_events` row
3. commits both together

Then a separate publisher reliably forwards outbox rows to Kafka. This gives eventual consistency with replayable publication attempts.

## Tradeoffs

- PostgreSQL is the source of truth, which simplifies recovery but adds DB load.
- At-least-once delivery is simpler and more honest than chasing “exactly once” claims.
- One application image for API, scheduler, and worker keeps the demo easy to run, but dedicated deployables could be cleaner in a larger system.
- Lease recovery reduces stuck work but can still cause duplicate business attempts if external side effects are not idempotent.
