package ru.prodvigaeff.control.modules.closedtasktime;

import ru.prodvigaeff.control.megaplan.managers.MegaplanTask;
import ru.prodvigaeff.control.model.Task;
import ru.prodvigaeff.control.utils.DateUtil;
import ru.prodvigaeff.control.utils.Logger;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ClosedTaskTimeChecker
{
    private static final int CHECK_HOURS_BACK = 24;

    public List<ClosedTaskTimeViolation> checkViolations()
    {
        Set<Task> tasks = MegaplanTask.getRecentTasksWithSubtasks();
        List<ClosedTaskTimeViolation> violations = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now(DateUtil.MSK_ZONE);
        LocalDateTime cutoffDate = now.minusHours(CHECK_HOURS_BACK);

        Logger.debug("Начинаем проверку учета времени в закрытых задачах");
        Logger.debug("Период: с " + cutoffDate + " по " + now + " (MSK)");

        int totalTasks = tasks.size();
        int processedTasks = 0;
        int closedTasksChecked = 0;

        for (Task task : tasks)
        {
            processedTasks++;

            if (!isClosedTask(task)) continue;

            closedTasksChecked++;

            if (task.getComments() == null || task.getComments().isEmpty()) continue;

            for (Task.TaskComment comment : task.getComments())
            {
                if (!comment.hasWorkTime() || comment.getWorkHours() <= 0) continue;

                if (comment.getCommentDate() == null) continue;

                LocalDateTime commentDateMsk = convertToMsk(comment.getCommentDate());

                if (commentDateMsk.isBefore(cutoffDate)) continue;

                Task.Employee violator = null;
                if (comment.getAuthor() != null && comment.getAuthor().getId() != null)
                {
                    String authorId = comment.getAuthor().getId();
                    violator = MegaplanTask.getEmployeeById(authorId);
                }

                ClosedTaskTimeViolation violation = new ClosedTaskTimeViolation(
                        task,
                        comment,
                        violator,
                        commentDateMsk,
                        comment.getWorkHours(),
                        task.getStatus()
                );

                violations.add(violation);

                Logger.warn("Нарушение: время в закрытой задаче - " +
                        "Задача: " + task.getId() +
                        ", Статус: " + task.getStatus() +
                        ", Сотрудник: " + (violator != null ? violator.getName() : "Неизвестен") +
                        ", Часы: " + comment.getWorkHours() +
                        ", Дата: " + comment.getCommentDate());

            }

            if (processedTasks % 50 == 0)
            {
                Logger.progress("Обработка задач", processedTasks, totalTasks);
            }
        }

        Logger.debug("Проверка завершена. Обработано задач: " + processedTasks +
                ", Закрытых задач проверено: " + closedTasksChecked +
                ", Нарушений найдено: " + violations.size());

        return violations;
    }

    private boolean isClosedTask(Task task)
    {
        if (task.getStatus() == null) return false;

        String status = task.getStatus();
        return "completed".equals(status) || "done".equals(status);
    }

    private LocalDateTime convertToMsk(LocalDateTime dateTime)
    {
        if (dateTime == null)
        {
            return null;
        }

        return dateTime.atZone(ZoneId.of("UTC"))
                .withZoneSameInstant(DateUtil.MSK_ZONE)
                .toLocalDateTime();
    }
}