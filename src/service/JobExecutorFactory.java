package service;

import model.JobType;

import java.util.HashMap;
import java.util.Map;

public class JobExecutorFactory {

    private final Map<JobType , JobExecutor> executorMap ;

    public JobExecutorFactory(Map<JobType , JobExecutor> executorMap){
        this.executorMap = executorMap ;
    }

    public JobExecutor getExecutor(JobType jobType){

        return executorMap.get(jobType);
    }
}
