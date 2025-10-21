package ru.prodvigaeff.control;

import ru.prodvigaeff.control.core.module.ModuleRegistry;
import ru.prodvigaeff.control.core.scheduler.Timer;
import ru.prodvigaeff.control.megaplan.managers.MegaplanTask;
import ru.prodvigaeff.control.modules.cache.CacheCleanupModule;
import ru.prodvigaeff.control.modules.closedtasktime.ClosedTaskTimeAuditModule;
import ru.prodvigaeff.control.modules.commentquality.CommentQualityAuditModule;
import ru.prodvigaeff.control.modules.taskhierarchy.TaskHierarchyAuditModule;
import ru.prodvigaeff.control.modules.tasktime.TaskTimeAuditModule;
import ru.prodvigaeff.control.modules.worktime.WorkTimeAuditModule;
import ru.prodvigaeff.control.service.EmailSender;
import ru.prodvigaeff.control.service.SimpleEmailSender;
import ru.prodvigaeff.control.service.SmtpClient;
import ru.prodvigaeff.control.service.TemplateProcessor;
import ru.prodvigaeff.control.utils.EnvUtil;
import ru.prodvigaeff.control.utils.Logger;

public class Application
{
    private static volatile boolean isShuttingDown = false;

    public static void main(String[] args)
    {
        try
        {
            initializeApplication();
        }
        catch (Exception e)
        {
            Logger.error("Критическая ошибка при запуске приложения: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void initializeApplication()
    {
        try
        {
            Logger.info("Загружаем конфигурацию...");
            EnvUtil.load();
            validateConfiguration();

            Logger.info("Регистрируем модули...");
            registerModules();

            setupShutdownHook();

            Logger.info("Запускаем планировщик...");
            Timer.start();

            Logger.success("Все модули зарегистрированы и планировщик запущен");
        }
        catch (ConfigurationException e)
        {
            Logger.error("Ошибка конфигурации WorkGuard: " + e.getMessage());
            System.exit(1);
        }
        catch (Exception e)
        {
            Logger.error("Неожиданная ошибка запуска WorkGuard: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void validateConfiguration()
    {
        Logger.debug("Проверяем обязательные параметры конфигурации...");

        String[] requiredParams = {
                "MEGAPLAN_API_KEY",
                "MEGAPLAN_URL",
                "SMTP_HOST",
                "SMTP_PORT",
                "SMTP_USERNAME",
                "SMTP_PASSWORD"
        };

        for (String param : requiredParams)
        {
            String value = EnvUtil.get(param);
            if (value == null || value.trim().isEmpty())
            {
                throw new ConfigurationException("Отсутствует обязательный параметр: " + param);
            }
        }

        try
        {
            int smtpPort = Integer.parseInt(EnvUtil.get("SMTP_PORT"));
            if (smtpPort < 1 || smtpPort > 65535)
            {
                throw new ConfigurationException("Некорректный SMTP_PORT: " + smtpPort);
            }
        }
        catch (NumberFormatException e)
        {
            throw new ConfigurationException("SMTP_PORT должен быть числом");
        }

        Logger.debug("Конфигурация валидна");
    }

    private static void registerModules()
    {
        try
        {
            TemplateProcessor templateProcessor = new TemplateProcessor();
            SmtpClient smtpClient = new SmtpClient();
            EmailSender emailSender = new SimpleEmailSender(templateProcessor, smtpClient);

            ModuleRegistry.register(new WorkTimeAuditModule(emailSender));
            ModuleRegistry.register(new TaskHierarchyAuditModule(emailSender));
            ModuleRegistry.register(new CommentQualityAuditModule(emailSender));
            ModuleRegistry.register(new TaskTimeAuditModule(emailSender));
            ModuleRegistry.register(new ClosedTaskTimeAuditModule(emailSender));

            ModuleRegistry.register(new CacheCleanupModule());

            int moduleCount = ModuleRegistry.getModuleCount();
            Logger.success("Зарегистрировано модулей: " + moduleCount);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Ошибка регистрации модулей", e);
        }
    }

    private static void setupShutdownHook()
    {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (isShuttingDown) return;

            isShuttingDown = true;

            Logger.info("Получен сигнал завершения");

            performGracefulShutdown();
        }));

        Logger.debug("Shutdown hook настроен");
    }

    private static void performGracefulShutdown()
    {
        long shutdownStart = System.currentTimeMillis();

        try
        {
            Logger.info("Останавливаем планировщик...");
            Timer.stop();

            Logger.info("Останавливаем MegaplanTask executor...");
            MegaplanTask.shutdown();

            Logger.info("Очищаем кеши...");
            MegaplanTask.clearEmployeeCache();

            long shutdownTime = System.currentTimeMillis() - shutdownStart;
            Logger.success("Работа корректно завершена за " + shutdownTime + "мс");
        }
        catch (Exception e)
        {
            Logger.error("Ошибка при завершении работы: " + e.getMessage());
        }
    }

    public static boolean isShuttingDown()
    {
        return isShuttingDown;
    }

    public static class ConfigurationException extends RuntimeException
    {
        public ConfigurationException(String message)
        {
            super(message);
        }

        public ConfigurationException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}