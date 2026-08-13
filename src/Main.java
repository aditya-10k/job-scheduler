import model.EmailJob ;
import model.Job;
import model.JobType;
import queue.JobPriorityComparator;
import service.EmailJobExecutor;
import service.EmailService;
import service.JobExecutor;
import service.JobExecutorFactory;
import worker.Worker;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

public class Main {

    public static void main(String[] args) throws InterruptedException {

        EmailJob job = new EmailJob(
                10,
                "receiver123",
                "system",
                "Welcome!",
                "Welcome to the platform"
        );

//        EmailJob job1 = new EmailJob(
//                20,
//                "receiver123",
//                "system",
//                "Welcome!",
//                "Welcome to the platform"
//        );
//
//        EmailJob job2 = new EmailJob(
//                3,
//                "receiver123",
//                "system",
//                "Welcome!",
//                "Welcome to the platform"
//        );

        EmailService emailService =
                new EmailService();



        EmailJobExecutor emailExecutor =
                new EmailJobExecutor(emailService);



        Map<JobType, JobExecutor> executorMap =
                new HashMap<>();

        executorMap.put(
                JobType.EMAIL,
                emailExecutor
        );

        JobExecutorFactory jobExecutorFactory = new JobExecutorFactory(executorMap);

        PriorityBlockingQueue<Job> pbq = new PriorityBlockingQueue<>(10 , new JobPriorityComparator());
        pbq.put(job);
//        pbq.put(job1);
//        pbq.put(job2);

        Thread worker1 = new Thread(new Worker(pbq , "worker1" , jobExecutorFactory));
        Thread worker2 = new Thread(new Worker(pbq , "worker2",jobExecutorFactory));
        Thread worker3 = new Thread(new Worker(pbq , "worker3",jobExecutorFactory));

        worker1.start();
//        worker2.start();
//        worker3.start();

        Thread.sleep(3000);

        System.out.println("Adding another job...");

        pbq.put(new EmailJob(
                100,
                "receiver4",
                "system",
                "Late job",
                "Important"
        ));

    }
}