package vn.fs.service.impl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import vn.fs.dto.MailInfo;
import vn.fs.service.SendMailService;

@Service
public class SendMailServiceImplement implements SendMailService {

    @Autowired
    JavaMailSender sender;

    private final List<MailInfo> list = new ArrayList<>();

    @Override
    public synchronized void queue(MailInfo mail) {
        list.add(mail);
    }

    @Override
    public void queue(String to, String subject, String body) {
        queue(new MailInfo(to, subject, body));
    }

    @Override
    public void send(MailInfo mail) throws MessagingException, IOException {
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        // From (nếu có)
        if (mail.getFrom() != null && !mail.getFrom().isBlank()) {
            helper.setFrom(mail.getFrom());
            // Nếu muốn có Reply-To y như From mà không cần field replyTo:
            helper.setReplyTo(mail.getFrom());
        }

        helper.setTo(mail.getTo());
        helper.setSubject(mail.getSubject());
        helper.setText(mail.getBody(), true);

        if (mail.getAttachments() != null && !mail.getAttachments().isBlank()) {
            FileSystemResource file = new FileSystemResource(new File(mail.getAttachments()));
            helper.addAttachment(file.getFilename(), file);
        }

        sender.send(message);
    }

    @Override
    @Scheduled(fixedDelay = 5000) // 5s quét 1 lần
    public void run() {
        while (true) {
            MailInfo mail;
            synchronized (this) {
                if (list.isEmpty()) break;
                mail = list.remove(0);
            }
            try {
                this.send(mail);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
