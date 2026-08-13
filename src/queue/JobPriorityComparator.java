package queue;

import model.Job;

import java.util.Comparator;

public class JobPriorityComparator implements Comparator<Job> {


    @Override
    public int compare(Job  job1, Job job2) {
        return Integer.compare(job2.getPriority() ,job1.getPriority());
    }
}
