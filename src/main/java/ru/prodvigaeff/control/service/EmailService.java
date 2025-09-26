package ru.prodvigaeff.control.service;

import ru.prodvigaeff.control.utils.EnvUtil;
import ru.prodvigaeff.control.utils.Logger;
import ru.prodvigaeff.control.utils.StringUtil;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

public class EmailService
{
    private static final int MAX_RETRY_ATTEMPTS = EnvUtil.getInt("EMAIL_MAX_RETRIES", 3);
    private static final int RETRY_DELAY_MS = EnvUtil.getInt("EMAIL_RETRY_DELAY_MS", 2000);

    public static void sendEmail(String toEmail, String subject, String templatePath, Map<String, String> placeholders)
    {
        sendEmailWithRetry(toEmail, subject, templatePath, placeholders, MAX_RETRY_ATTEMPTS);
    }

    public static void sendEmailWithRetry(String toEmail, String subject, String templatePath, Map<String, String> placeholders, int maxAttempts)
    {
        ValidationResult validation = validateEmailParameters(toEmail, subject, templatePath, placeholders);
        if (!validation.isValid)
        {
            Logger.error("Валидация email параметров не прошла: " + validation.errorMessage);
            throw new EmailValidationException(validation.errorMessage);
        }

        Logger.debug("TO", StringUtil.maskEmail(toEmail));
        Logger.debug("SUBJECT", subject);
        Logger.debug("TEMPLATE", templatePath);

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++)
        {
            try
            {
                Logger.debug("ATTEMPT", attempt + "/" + maxAttempts);
                sendEmailAttempt(toEmail, subject, templatePath, placeholders);
                Logger.success("Email успешно отправлен: " + StringUtil.maskEmail(toEmail));
                return;
            }
            catch (EmailException e)
            {
                lastException = e;
                Logger.error("Попытка " + attempt + "/" + maxAttempts + " не удалась: " + e.getMessage());

                if (attempt < maxAttempts)
                {
                    sleepBeforeRetry(attempt);
                }
            }
        }

        Logger.error("Не удалось отправить email после " + maxAttempts + " попыток");
        throw new EmailException("Email не отправлен после " + maxAttempts + " попыток", lastException);
    }

    private static void sendEmailAttempt(String toEmail, String subject, String templatePath,
                                         Map<String, String> placeholders) throws EmailException
    {
        try
        {
            Logger.debug("STEP 1", "Загружаем шаблон");
            String htmlContent = loadTemplate(templatePath);
            Logger.debug("TEMPLATE_SIZE", String.valueOf(htmlContent.length()));

            Logger.debug("STEP 2", "Заменяем плейсхолдеры");
            htmlContent = replacePlaceholders(htmlContent, placeholders);
            Logger.debug("FINAL_SIZE", String.valueOf(htmlContent.length()));

            Logger.debug("STEP 3", "Создаем SMTP сессию");
            Session session = createMailSession();

            Logger.debug("STEP 4", "Создаем сообщение");
            MimeMessage message = createMessage(session, toEmail, subject, htmlContent);

            Logger.debug("STEP 5", "Отправляем сообщение");
            Transport.send(message);
        }
        catch (MessagingException e)
        {
            throw new EmailException("Ошибка отправки email", e);
        }
        catch (IOException e)
        {
            throw new EmailException("Ошибка загрузки шаблона", e);
        }
        catch (Exception e)
        {
            throw new EmailException("Неожиданная ошибка при отправке email", e);
        }
    }

    private static ValidationResult validateEmailParameters(String toEmail, String subject,
                                                            String templatePath, Map<String, String> placeholders)
    {
        ValidationResult result = new ValidationResult();

        if (!StringUtil.isValidEmail(toEmail))
        {
            result.setError("Некорректный email получателя: " + toEmail);
            return result;
        }

        if (StringUtil.isEmpty(subject))
        {
            result.setError("Пустая тема письма");
            return result;
        }

        if (subject.length() > 200)
        {
            result.setError("Тема письма слишком длинная: " + subject.length() + " символов");
            return result;
        }

        if (StringUtil.isEmpty(templatePath))
        {
            result.setError("Не указан путь к шаблону");
            return result;
        }

        Path template = Paths.get(templatePath);
        if (!Files.exists(template))
        {
            result.setError("Шаблон не найден: " + templatePath);
            return result;
        }

        if (placeholders == null)
        {
            result.setError("Плейсхолдеры не могут быть null");
            return result;
        }

        result.isValid = true;
        return result;
    }

    private static String loadTemplate(String templatePath) throws IOException
    {
        Path template = Paths.get(templatePath);
        return Files.readString(template);
    }

    private static String replacePlaceholders(String content, Map<String, String> placeholders)
    {
        if (placeholders == null || placeholders.isEmpty()) return content;

        String result = content;
        for (Map.Entry<String, String> entry : placeholders.entrySet())
        {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";

            String escapedValue = escapeHtml(value);
            result = result.replace("{" + key + "}", escapedValue);
        }

        if (result.contains("{") && result.contains("}"))
        {
            Logger.warn("В шаблоне остались незамененные плейсхолдеры");
        }

        return result;
    }

    private static String escapeHtml(String text)
    {
        if (StringUtil.isEmpty(text)) return text;

        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static MimeMessage createMessage(Session session, String toEmail, String subject,
                                             String htmlContent) throws MessagingException
    {
        MimeMessage message = new MimeMessage(session);

        String fromEmail = EnvUtil.get("SMTP_USERNAME");
        if (!StringUtil.isValidEmail(fromEmail))
        {
            throw new MessagingException("Некорректный email отправителя: " + fromEmail);
        }
        message.setFrom(new InternetAddress(fromEmail));

        try
        {
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        }
        catch (AddressException e)
        {
            throw new MessagingException("Некорректный email получателя: " + toEmail, e);
        }

        String adminEmails = EnvUtil.get("EMAIL_ADMIN");
        if (StringUtil.isNotEmpty(adminEmails))
        {
            try
            {
                message.setRecipients(Message.RecipientType.CC, InternetAddress.parse(adminEmails));
                Logger.debug("ADMIN_CC", StringUtil.maskEmail(adminEmails));
            }
            catch (AddressException e)
            {
                Logger.warn("Некорректные email админов, пропускаем CC: " + adminEmails);
            }
        }

        message.setSubject(subject);
        message.setContent(htmlContent, "text/html; charset=utf-8");

        return message;
    }

    private static Session createMailSession()
    {
        Properties props = new Properties();

        String smtpHost = EnvUtil.get("SMTP_HOST");
        String smtpPort = EnvUtil.get("SMTP_PORT");
        String smtpUsername = EnvUtil.get("SMTP_USERNAME");
        String smtpPassword = EnvUtil.get("SMTP_PASSWORD");

        if (StringUtil.isEmpty(smtpHost) || StringUtil.isEmpty(smtpPort) ||
                StringUtil.isEmpty(smtpUsername) || StringUtil.isEmpty(smtpPassword))
        {
            throw new EmailConfigurationException("Не настроены SMTP параметры");
        }

        props.put("mail.smtp.host", smtpHost);
        props.put("mail.smtp.port", smtpPort);
        props.put("mail.smtp.auth", "true");

        if ("465".equals(smtpPort))
        {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.trust", "*");
        }
        else props.put("mail.smtp.starttls.enable", "true");

        props.put("mail.smtp.connectiontimeout", EnvUtil.get("SMTP_CONNECTION_TIMEOUT", "10000"));
        props.put("mail.smtp.timeout", EnvUtil.get("SMTP_TIMEOUT", "10000"));

        return Session.getInstance(props, new Authenticator()
        {
            @Override
            protected PasswordAuthentication getPasswordAuthentication()
            {
                return new PasswordAuthentication(smtpUsername, smtpPassword);
            }
        });
    }

    private static void sleepBeforeRetry(int attempt)
    {
        try
        {
            int delay = RETRY_DELAY_MS * attempt;
            Thread.sleep(delay);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            Logger.warn("Прервана задержка перед повтором отправки email");
        }
    }

    private static class ValidationResult
    {
        boolean isValid = false;
        String errorMessage = "";

        void setError(String message)
        {
            this.isValid = false;
            this.errorMessage = message;
        }
    }

    public static class EmailException extends RuntimeException
    {
        public EmailException(String message)
        {
            super(message);
        }

        public EmailException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    public static class EmailValidationException extends RuntimeException
    {
        public EmailValidationException(String message)
        {
            super(message);
        }
    }

    public static class EmailConfigurationException extends RuntimeException
    {
        public EmailConfigurationException(String message)
        {
            super(message);
        }
    }
}