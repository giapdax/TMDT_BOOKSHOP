package vn.fs.dto;

public class MailInfo {
    private String from;        // người gửi (optional)
    private String replyTo;     // email nhận phản hồi (optional)
    private String to;          // người nhận
    private String subject;     // tiêu đề
    private String body;        // nội dung (HTML)
    private String attachments; // đường dẫn file đính kèm (optional)

    public MailInfo() {
    }

    // Hay dùng nhất: chỉ cần to/subject/body
    public MailInfo(String to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    // Có from nhưng không có replyTo
    public MailInfo(String from, String to, String subject, String body) {
        this.from = from;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    // Full option
    public MailInfo(String from, String replyTo, String to, String subject, String body, String attachments) {
        this.from = from;
        this.replyTo = replyTo;
        this.to = to;
        this.subject = subject;
        this.body = body;
        this.attachments = attachments;
    }

    // Getters/Setters
    public String getFrom() {
        return from;
    }
    public void setFrom(String from) {
        this.from = from;
    }

    public String getReplyTo() {
        return replyTo;
    }
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    public String getTo() {
        return to;
    }
    public void setTo(String to) {
        this.to = to;
    }

    public String getSubject() {
        return subject;
    }
    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }
    public void setBody(String body) {
        this.body = body;
    }

    public String getAttachments() {
        return attachments;
    }
    public void setAttachments(String attachments) {
        this.attachments = attachments;
    }
}
