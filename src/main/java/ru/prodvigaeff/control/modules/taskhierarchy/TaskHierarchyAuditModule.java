package ru.prodvigaeff.control.modules.taskhierarchy;

import ru.prodvigaeff.control.core.module.AbstractModule;
import ru.prodvigaeff.control.service.EmailSender;
import ru.prodvigaeff.control.utils.Logger;

import java.util.List;

public class TaskHierarchyAuditModule extends AbstractModule
{
    private final TaskHierarchyChecker checker;
    private final TaskHierarchyNotifier notifier;

    public TaskHierarchyAuditModule(EmailSender emailSender)
    {
        this.checker = new TaskHierarchyChecker();
        this.notifier = new TaskHierarchyNotifier(emailSender);
    }

    @Override
    public String getName()
    {
        return "TaskHierarchyValidator";
    }

    @Override
    public String getCronExpression()
    {
        return "0 55 11 * * *";
    }

    @Override
    public void executeModule()
    {
        Logger.debug("Начинаем проверку дат создания задач и подзадач");

        long startTime = System.currentTimeMillis();
        List<TaskHierarchyViolation> violations = checker.checkViolations();

        if (violations.isEmpty()) Logger.debug("Нарушений дат создания не найдено");

        else
        {
            Logger.debug("Найдено нарушений дат создания: " + violations.size());
            notifier.sendNotifications(violations);
            Logger.debug("Все уведомления о нарушениях отправлены");
        }

        long endTime = System.currentTimeMillis();
        Logger.result("Проверка дат создания задач", violations.size(), endTime - startTime);
    }
}