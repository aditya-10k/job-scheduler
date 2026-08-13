package model ;
public class EmailJob extends Job{

    private final String senderId ;
    private final String receiverId ;
    private final String body ;
    private final String subject ;

    public EmailJob (
            int priority ,
            String receiverId ,
            String senderId ,
            String body ,
            String subject
    ){
        super(priority , JobType.EMAIL);
        this.body = body ;
        this.senderId = senderId ;
        this.receiverId = receiverId ;
        this.subject = subject ;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }

    public String getSenderId() {
        return senderId;
    }
}
