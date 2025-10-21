package ru.prodvigaeff.control.modules.worktime;

import ru.prodvigaeff.control.megaplan.managers.MegaplanTask;
import ru.prodvigaeff.control.model.Task;
import ru.prodvigaeff.control.utils.DateUtil;
import ru.prodvigaeff.control.utils.Logger;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WorkTimeChecker
{
    public List<WorkTimeViolation> checkViolations(LocalDateTime currentTime)
    {
        Set<Task> tasks = MegaplanTask.getRecentTasksWithSubtasks();
        List<WorkTimeViolation> violations = new ArrayList<>();

        ZonedDateTime currentMsk = currentTime.atZone(DateUtil.MSK_ZONE);
        LocalDateTime checkStart = currentMsk.minusHours(24).toLocalDateTime();
        LocalDateTime checkEnd = currentMsk.toLocalDateTime();

        Logger.debug("Начинаем проверку нарушений рабочего времени");
        Logger.debug("Проверяем период с " + checkStart + " по " + checkEnd);

        int processedTasks = 0;
        int totalTasks = tasks.size();

        for (Task task : tasks)
        {
            processedTasks++;
            Logger.progress("Проверка задач", processedTasks, totalTasks);

            if (task == null || task.getComments() == null || task.getComments().isEmpty()) continue;

            for (Task.TaskComment comment : task.getComments())
            {
                if (isViolation(comment, checkStart, checkEnd))
                {
                    int daysDiff = Math.abs(DateUtil.daysDifference(
                        comment.getCommentDate().toLocalDate(),
                        comment.getWorkDate().toLocalDate()
                    ));
                    violations.add(new WorkTimeViolation(task, comment, daysDiff));
                }
            }
        }

        Logger.debug("Проверка завершена. Найдено нарушений: " + violations.size());
        return violations;
    }

    private boolean isViolation(Task.TaskComment comment, LocalDateTime checkStart, LocalDateTime checkEnd)
    {
        if (!comment.hasWorkTime() || comment.getWorkHours() <= 0) return false;

        LocalDateTime commentDate = comment.getCommentDate();
        LocalDateTime workDate = comment.getWorkDate();

        if (commentDate == null || workDate == null) return false;
        
        if (commentDate.isBefore(checkStart) || commentDate.isAfter(checkEnd)) return false;

        return !DateUtil.isSameDate(commentDate, workDate);
    }
}
