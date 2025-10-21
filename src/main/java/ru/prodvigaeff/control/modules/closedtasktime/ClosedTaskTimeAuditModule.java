package ru.prodvigaeff.control.modules.closedtasktime;

import ru.prodvigaeff.control.core.module.AbstractModule;
import ru.prodvigaeff.control.service.EmailSender;
import ru.prodvigaeff.control.utils.Logger;

import java.util.List;

public class ClosedTaskTimeAuditModule
extends AbstractModule
{
    private final ClosedTaskTimeChecker checker;
    private final ClosedTaskTimeNotifier notifier;

    public ClosedTaskTimeAuditModule(EmailSender emailSender)
    {
        this.checker = new ClosedTaskTimeChecker();
        this.notifier = new ClosedTaskTimeNotifier(emailSender);
    }

    @Override
    public String getName()
    {
        return "ClosedTaskTimeAudit";
    }

    @Override
    public String getCronExpression()
    {
        return "0 55 13 * * *";
    }
    @Override
    public void executeModule()
    {
        Logger.debug("Начинаем аудит времени в закрытых задачах");

        long startTime = System.currentTimeMillis();
        List<ClosedTaskTimeViolation> violations = checker.checkViolations();

        if (violations.isEmpty())
        {
            Logger.debug("Нарушений не найдено");
        }
        else
        {
            Logger.warn("Найдено нарушений: " + violations.size());
            notifier.sendNotifications(violations);
            Logger.debug("Все уведомления отправлены");
        }

        long endTime = System.currentTimeMillis();
        Logger.result("Аудит времени в закрытых задачах", violations.size(), endTime - startTime);
    }
}
