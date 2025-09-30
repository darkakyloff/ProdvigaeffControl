package ru.prodvigaeff.control.modules.commentquality;

import ru.prodvigaeff.control.megaplan.managers.MegaplanTask;
import ru.prodvigaeff.control.model.Task;
import ru.prodvigaeff.control.utils.DateUtil;
import ru.prodvigaeff.control.utils.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CommentQualityChecker
{
    private static final double MIN_HOURS_TO_CHECK = 1.0;
    private final AIAnalysisService aiService;

    public CommentQualityChecker()
    {
        this.aiService = new AIAnalysisService();
    }

    public List<CommentQualityViolation> checkViolations()
    {
        Set<Task> tasks = MegaplanTask.getAllTasks();
        List<CommentQualityViolation> violations = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now(DateUtil.MSK_ZONE);
        LocalDateTime cutoffDate = now.minusHours(24);

        Logger.info("Начинаем проверку качества комментариев");
        Logger.info("Проверяем комментарии с >= " + MIN_HOURS_TO_CHECK + " часов за последние 7 дней");
        Logger.info("Период с " + cutoffDate + " по " + now + " (MSK)");

        int checkedComments = 0;
        int totalTasks = tasks.size();
        int processedTasks = 0;

        for (Task task : tasks)
        {
            processedTasks++;

            if (task.getComments() == null || task.getComments().isEmpty()) continue;

            for (Task.TaskComment comment : task.getComments())
            {
                String authorName = "Неизвестен";
                String authorId = "?";
                if (comment.getAuthor() != null && comment.getAuthor().getId() != null)
                {
                    authorId = comment.getAuthor().getId();
                    Task.Employee fullEmployee = MegaplanTask.getEmployeeById(authorId);
                    if (fullEmployee != null && fullEmployee.getName() != null)
                    {
                        authorName = fullEmployee.getName();
                    }
                }

                if (comment.getCommentDate() == null)
                {
                    Logger.debug("Пропущен (нет даты): задача=" + task.getId() + ", автор=" + authorName);continue;
                }

                LocalDateTime commentDateMsk = convertToMsk(comment.getCommentDate());

                if (commentDateMsk.isBefore(cutoffDate))
                {
                    Logger.debug("Пропущен (старая дата " + commentDateMsk + " MSK): задача=" + task.getId() + ", автор=" + authorName);continue;
                }

                if (!comment.hasWorkTime())
                {
                    Logger.debug("Пропущен (нет workTime): задача=" + task.getId() + ", автор=" + authorName);continue;
                }

                if (comment.getWorkHours() < MIN_HOURS_TO_CHECK)
                {
                    Logger.debug("Пропущен (мало часов " + comment.getWorkHours() + "ч): задача=" + task.getId() + ", автор=" + authorName);continue;
                }

                if (comment.getContent() == null || comment.getContent().trim().isEmpty())
                {
                    Logger.debug("Пропущен (нет текста): задача=" + task.getId() + ", автор=" + authorName + ", часы=" + comment.getWorkHours());continue;
                }

                checkedComments++;
                Logger.debug("Проверяем комментарий #" + checkedComments +
                        ": задача=" + task.getId() +
                        ", автор=" + authorName +
                        ", дата=" + commentDateMsk + " MSK" +
                        ", часы=" + comment.getWorkHours());

                try
                {
                    AIAnalysisResult result = aiService.analyze(
                            comment.getContent(),
                            comment.getWorkHours(),
                            comment.getAuthor(),
                            task.getName()
                    );

                    if (result == null)
                    {
                        Logger.debug("Комментарий пропущен из-за ошибки AI"); continue;
                    }

                    if ("FAIL".equals(result.getVerdict()))
                    {
                        violations.add(new CommentQualityViolation(task, comment, result));
                        Logger.warn("Нарушение: задача " + task.getId() +
                                ", автор: " + authorName +
                                ", оценка: " + result.getTotalScore());
                    }
                    else Logger.success("Комментарий прошел проверку (автор: " + authorName + ")");

                }
                catch (Exception e)
                {
                    Logger.error("Ошибка анализа комментария (автор: " + authorName + "): " + e.getMessage());
                }
            }

            if (processedTasks % 50 == 0)
            {
                Logger.progress("Обработка задач", processedTasks, totalTasks);
            }
        }


        return violations;
    }

    private LocalDateTime convertToMsk(LocalDateTime dateTime)
    {
        if (dateTime == null) return null;

        return dateTime.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(DateUtil.MSK_ZONE)
                .toLocalDateTime();
    }
}