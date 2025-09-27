package ru.prodvigaeff.control.service;

import java.util.Map;

public interface EmailSender
{
    void sendEmail(String recipient, String subject, String templateName, Map<String, String> data);
}