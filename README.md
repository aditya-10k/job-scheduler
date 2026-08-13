# Job Scheduler

A simple, multi-threaded Job Scheduler implemented in Java. It demonstrates how to manage and execute prioritized jobs concurrently using multiple worker threads.

## Features

- **Priority-based Job Execution**: Jobs are processed based on their assigned priority using a `PriorityBlockingQueue`.
- **Concurrent Processing**: Multiple worker threads can pull from the job queue simultaneously, allowing concurrent execution.
- **Extensible Architecture**: Uses the Factory design pattern (`JobExecutorFactory`) and interfaces (`Job`, `JobExecutor`) making it easy to add new job types (like SMS, Push Notifications) without changing the core worker logic.
- **Example Implementation**: Includes an `EmailJob` implementation to demonstrate the system's capabilities out-of-the-box.

## Project Structure

- `model/`: Contains job representations (e.g., `Job`, `EmailJob`, `JobType`).
- `queue/`: Contains the queue and sorting logic, such as `JobPriorityComparator`.
- `service/`: Houses the business logic and factories for executing jobs (e.g., `EmailService`, `EmailJobExecutor`, `JobExecutorFactory`).
- `worker/`: Contains the `Worker` class that runs on separate threads to poll and execute jobs from the queue.
- `Main.java`: The entry point demonstrating how to configure and run the workers with the queue.

## How to Run

1. Open the project in your favorite IDE (like IntelliJ IDEA or Eclipse).
2. Run the `Main.java` class.
3. Observe the console output as workers pick up and execute the simulated email jobs based on their priority.
