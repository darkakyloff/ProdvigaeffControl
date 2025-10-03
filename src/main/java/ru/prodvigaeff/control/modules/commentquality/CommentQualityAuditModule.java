package ru.prodvigaeff.control.modules.commentquality;

import ru.prodvigaeff.control.core.module.AbstractModule;
import ru.prodvigaeff.control.service.EmailSender;
import ru.prodvigaeff.control.utils.Logger;

import java.util.List;

public class CommentQualityAuditModule extends AbstractModule
{
    private final CommentQualityChecker checker;
    private final CommentQualityNotifier notifier;

    public CommentQualityAuditModule(EmailSender emailSender)
    {
        this.checker = new CommentQualityChecker();
        this.notifier = new CommentQualityNotifier(emailSender);
    }

    @Override
    public String getName()
    {
        return "CommentQualityAuditor";
    }

    @Override
    public String getCronExpression()
    {
        return "0 55 12 * * *";
    }

    @Override
    public void executeModule()
    {
        Logger.debug("Начинаем аудит качества комментариев");

        long startTime = System.currentTimeMillis();
        List<CommentQualityViolation> violations = checker.checkViolations();

        if (violations.isEmpty())
        {
            Logger.debug("Нарушений качества комментариев не найдено");
        }
        else
        {
            Logger.warn("Найдено нарушений качества комментариев: " + violations.size());
            notifier.sendNotifications(violations);
            Logger.debug("Все уведомления о нарушениях отправлены");
        }

        long endTime = System.currentTimeMillis();
        Logger.result("Аудит качества комментариев", violations.size(), endTime - startTime);
    }
}