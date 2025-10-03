package ru.prodvigaeff.control.modules.tasktime;

import ru.prodvigaeff.control.core.module.AbstractModule;
import ru.prodvigaeff.control.model.Task;
import ru.prodvigaeff.control.modules.taskhierarchy.TaskHierarchyNotifier;
import ru.prodvigaeff.control.service.EmailSender;
import ru.prodvigaeff.control.utils.Logger;

import java.util.List;

public class TaskTimeAuditModule
extends AbstractModule
{
    private final TaskTimeChecker checker;
    private final TaskTimeNotifier notifier;

    public TaskTimeAuditModule (EmailSender emailSender)
    {
        this.checker = new TaskTimeChecker();
        this.notifier = new TaskTimeNotifier(emailSender);
    }

    @Override
    public String getName()
    {
        return "TaskTimeAuditor";
    }

    @Override
    public String getCronExpression()
    {
        return "0 55 10 * * *";
    }

    @Override
    public void executeModule()
    {
        Logger.debug("Начинаем проверку соответствия плановых часов к фактическим ");

        long startTime = System.currentTimeMillis();

        List<Task> violations = checker.checkViolations();

        if (violations.isEmpty()) Logger.debug("Нарушений не соответствия плановых часов к фактическим создания не найдено");
        else
        {
            Logger.debug("Найдено нарушений: " + violations.size());
            notifier.sendNotification(violations);
            Logger.debug("Все уведомления отправлены");
        }

        long endTime = System.currentTimeMillis();
        Logger.result("Проверка соответствия плановых часов к фактическим", violations.size(), endTime - startTime);

    }
}
