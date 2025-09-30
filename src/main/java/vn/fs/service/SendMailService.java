package vn.fs.service;

import java.io.IOException;
import javax.mail.MessagingException;

import vn.fs.dto.MailInfo;

/**
 * Hàng đợi mail nhẹ nhàng (queue + @Scheduled).
 * Lưu ý: KHÔNG đặt @Service trên interface này.
 */
public interface SendMailService {
    void run(); // scheduler: gửi dần mail trong queue

    void queue(String to, String subject, String body);

    void queue(MailInfo mail);

    void send(MailInfo mail) throws MessagingException, IOException;
}
