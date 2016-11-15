package allure.report.email

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Created by naitse on 11/13/16.
 */
public class HTMLEmail {

    HTMLEmail() {
    }

    public void send(from, password, to, subject, content) {

        System.out.println(to)
        System.out.println(from)

        Properties properties = System.getProperties();
        properties.put("mail.smtp.starttls.enable", "true");
        properties.setProperty("mail.transport.protocol", "smtp");
        properties.setProperty("mail.host", "smtp.gmail.com");
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.port", "465");
        properties.put("mail.debug", "true");
        properties.put("mail.smtp.socketFactory.port", "465");
        properties.put("mail.smtp.socketFactory.class","javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.socketFactory.fallback", "false");
        Session session = Session.getDefaultInstance(properties,
                new javax.mail.Authenticator() {
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(from,password);
                    }
                });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from as String, "ARM Automation"));
            List recipients = to.split(',')
            recipients.each {
                message.addRecipient(Message.RecipientType.TO, new InternetAddress(it));
            }
            message.setSubject(subject);
            message.setContent(content, "text/html");
            Transport.send(message);
            System.out.println("Sent message successfully....");
        }catch (MessagingException mex) {
            mex.printStackTrace();
        }
    }
}