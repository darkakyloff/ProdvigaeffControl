package ru.prodvigaeff.control.service;

import ru.prodvigaeff.control.utils.EnvUtil;
import ru.prodvigaeff.control.utils.StringUtil;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

public class SmtpClient
{
    public void send(String recipient, String subject, String body)
    {
        Session session = createSession();
        MimeMessage message = createMessage(session, recipient, subject, body);
        
        try
        {
            Transport.send(message);
        }
        catch (MessagingException e)
        {
            throw new RuntimeException("Ошибка отправки email", e);
        }
    }

    private Session createSession()
    {
        Properties props = new Properties();
        
        String smtpHost = EnvUtil.get("SMTP_HOST");
        String smtpPort = EnvUtil.get("SMTP_PORT");
        String smtpUsername = EnvUtil.get("SMTP_USERNAME");
        String smtpPassword = EnvUtil.get("SMTP_PASSWORD");

        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");

        if ("465".equals(smtpPort))
        {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", "*");
        }
        else
        {
            props.put("mail.smtp.starttls.enable", "true");
        }

        return Session.getInstance(props, new Authenticator()
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });
    }

    private MimeMessage createMessage(Session session, String recipient, String subject, String body)
    {
        try
        {
            MimeMessage message = new MimeMessage(session);
            
            String fromEmail = EnvUtil.get("SMTP_USERNAME");
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipient));
            
            String adminEmails = EnvUtil.get("EMAIL_ADMIN");
            if (StringUtil.isNotEmpty(adminEmails))
            {
                message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(adminEmails));
            }

            message.setSubject(subject);
            message.setContent(body, "text/html; charset=utf-8");
            
            return message;
        }
        catch (MessagingException e)
        {
            throw new RuntimeException("Ошибка создания сообщения", e);
        }
    }
}