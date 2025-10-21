package ru.prodvigaeff.control.modules.taskhierarchy;

import ru.prodvigaeff.control.megaplan.managers.MegaplanTask;
import ru.prodvigaeff.control.model.Task;
import ru.prodvigaeff.control.utils.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TaskHierarchyChecker
{
    private static final int MAX_ALLOWED_HOURS_DIFF = 12;

    public List<TaskHierarchyViolation> checkViolations()
    {
        LocalDateTime now = LocalDateTime.now();
        Set<Task> allTasks = MegaplanTask.getRecentTasksWithSubtasks();
        List<TaskHierarchyViolation> violations = new ArrayList<>();

        LocalDateTime cutoffDate = now.minusHours(24);
        List<Task> recentTasks = allTasks.stream()
                .filter(task -> task.getActivity() != null && task.getActivity().isAfter(cutoffDate))
                .filter(task -> task.getSubtaskIds() != null && !task.getSubtaskIds().isEmpty())
                .collect(Collectors.toList());

        Logger.debug("Начинаем проверку дат создания задач и подзадач");
        Logger.debug("Всего задач: " + allTasks.size() + ", активных с подзадачами: " + recentTasks.size());

        for (Task parentTask : recentTasks)
        {
            for (String subtaskId : parentTask.getSubtaskIds())
            {
                Task subtask = MegaplanTask.getTaskById(subtaskId);
                if (subtask == null || subtask.getTimeCreated() == null) continue;

                if (subtask.getTimeCreated().isBefore(parentTask.getTimeCreated()))
                {
                    long hoursDiff = java.time.Duration.between(subtask.getTimeCreated(), parentTask.getTimeCreated()).toHours();

                    if (hoursDiff > MAX_ALLOWED_HOURS_DIFF)
                    {
                        violations.add(new TaskHierarchyViolation(parentTask, subtask, hoursDiff));
                    }
                }
            }
        }

        Logger.debug("Проверка завершена. Найдено нарушений: " + violations.size());
        return violations;
    }
}