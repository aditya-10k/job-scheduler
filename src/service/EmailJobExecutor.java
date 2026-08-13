package service;

import model.EmailJob;
import model.Job;

public class EmailJobExecutor implements JobExecutor{

    private EmailService emailService ;

    public EmailJobExecutor(EmailService service){
        this.emailService = service ;
    }

    @Override
    public void execute(Job job){

        EmailJob emailJob = (EmailJob) job ;
        emailService.sendMail(emailJob.getSenderId(), emailJob.getReceiverId() , emailJob.getSubject() , emailJob.getBody());
    }
}
