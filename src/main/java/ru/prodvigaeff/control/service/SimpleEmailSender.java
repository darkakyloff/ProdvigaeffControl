package ru.prodvigaeff.control.service;

import java.util.Map;

public class SimpleEmailSender
implements EmailSender
{
    private final TemplateProcessor templateProcessor;
    private final SmtpClient smtpClient;
    
    public SimpleEmailSender(TemplateProcessor templateProcessor, SmtpClient smtpClient)
    {
        this.templateProcessor = templateProcessor;
        this.smtpClient = smtpClient;
    }
    
    @Override
    public void sendEmail(String recipient, String subject, String templateName, Map<String, String> data)
    {
        String emailBody = templateProcessor.processTemplate(templateName, data);
        smtpClient.send(recipient, subject, emailBody);
    }
}