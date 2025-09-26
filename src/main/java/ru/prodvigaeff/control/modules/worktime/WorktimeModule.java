package ru.prodvigaeff.control.modules.worktime;

import ru.prodvigaeff.control.core.module.AbstractModule;
import ru.prodvigaeff.control.model.Task;
import ru.prodvigaeff.control.utils.Logger;

import java.util.HashMap;

public class WorktimeModule extends AbstractModule
{
    @Override
    public String getName()
    {
        return "WorktimeAuditor";
    }

    @Override
    public String getCronExpression()
    {
        return "0 00 10 * * *";
    }

    @Override
    public void executeModule()
    {
        Logger.info("Начинаем аудит рабочего времени");

        long startTime = System.currentTimeMillis();

        HashMap<Task, Task.TaskComment> violations = WorkTimeExecutor.getViolation();

        if (violations.isEmpty()) Logger.info("Нарушений не найдено");

        else
        {
            Logger.info("Найдено нарушений: " + violations.size());

            Logger.info("Отправляем email уведомления");
            WorkTimeExecutor.sendEmailMessages(violations);
            Logger.success("Все уведомления отправлены");
        }

        long endTime = System.currentTimeMillis();
        Logger.result("Аудит рабочего времени", violations.size(), endTime - startTime);
    }
}