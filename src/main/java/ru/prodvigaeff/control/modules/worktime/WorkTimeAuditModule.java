package ru.prodvigaeff.control.modules.worktime;

import ru.prodvigaeff.control.core.module.AbstractModule;
import ru.prodvigaeff.control.service.EmailSender;
import ru.prodvigaeff.control.utils.Logger;

import java.time.LocalDateTime;
import java.util.List;

public class WorkTimeAuditModule extends AbstractModule
{
    private final WorkTimeChecker checker;
    private final WorkTimeNotifier notifier;

    public WorkTimeAuditModule(EmailSender emailSender)
    {
        this.checker = new WorkTimeChecker();
        this.notifier = new WorkTimeNotifier(emailSender);
    }

    @Override
    public String getName()
    {
        return "WorkTimeAuditor";
    }

    @Override
    public String getCronExpression()
    {
        return "0 55 9 * * *";
    }

    @Override
    public void executeModule()
    {
        Logger.info("Начинаем аудит рабочего времени");

        long startTime = System.currentTimeMillis();
        List<WorkTimeViolation> violations = checker.checkViolations(LocalDateTime.now());

        if (violations.isEmpty())
        {
            Logger.info("Нарушений не найдено");
        }
        else
        {
            Logger.info("Найдено нарушений: " + violations.size());
            notifier.sendNotifications(violations);
            Logger.success("Все уведомления отправлены");
        }

        long endTime = System.currentTimeMillis();
        Logger.result("Аудит рабочего времени", violations.size(), endTime - startTime);
    }
}