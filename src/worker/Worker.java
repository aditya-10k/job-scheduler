package worker;

import model.Job;
import model.JobStatus;
import model.JobType;
import service.JobExecutor;
import service.JobExecutorFactory;

import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;

public class Worker implements Runnable{

    private final PriorityBlockingQueue<Job> pbq ;
    private final String workerName ;
    private final JobExecutorFactory jobExecutorFactory ;

    public Worker(PriorityBlockingQueue<Job> pbq , String name , JobExecutorFactory jobExecutorFactory){
        this.pbq = pbq ;
        this.workerName = name;
        this.jobExecutorFactory = jobExecutorFactory ;
    }


    @Override
    public void run() {

        while (true){
            try {
                Job job = pbq.take();
                job.setStatus(JobStatus.EXECUTING);
                try {
                System.out.println("Job : " + job.getId() + " \t" +"Priority"+ job.getPriority() +"\t"+ "Worker"+ workerName);

                JobExecutor jobExecutor = jobExecutorFactory.getExecutor(job.getJobType());
                jobExecutor.execute(job);
                job.setStatus(JobStatus.FINISHED);
                }catch (Exception e){
                    System.out.println("Error : " +e.toString() );
                    job.setStatus(JobStatus.FAILED);
                }
            }
            catch (InterruptedException e){
                Thread.currentThread().interrupt();
                break;
            }

        }
    }
}
