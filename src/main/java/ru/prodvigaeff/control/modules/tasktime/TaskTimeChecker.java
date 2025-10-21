package ru.prodvigaeff.control.modules.tasktime;

import ru.prodvigaeff.control.megaplan.managers.MegaplanTask;
import ru.prodvigaeff.control.model.Task;
import ru.prodvigaeff.control.utils.EnvUtil;
import ru.prodvigaeff.control.utils.Logger;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TaskTimeChecker
{
    public List<Task> checkViolations()
    {
        LocalDateTime now = LocalDateTime.now();

        Set<Task> allTasks = MegaplanTask.getRecentTasksWithSubtasks();

        LocalDateTime cutoffDate = now.minusHours(24);
        List<Task> recentTasks = allTasks.stream()
                .filter(task -> task.getActivity() != null && task.getActivity().isAfter(cutoffDate)).toList();

        Logger.debug("Всего задач: " + allTasks.size() + ", активных за 24 часа: " + recentTasks.size());

        List <Task> violations = new ArrayList<>();

        for (Task task : recentTasks)
        {
            if (task.getActualWorkHours() > task.getPlannedWorkHours())
            {
                String taskNameLower = task.getName().toLowerCase();

                if (taskNameLower.contains("проект") && taskNameLower.contains("регулярной оплатой") || taskNameLower.contains("период")) continue;

                if (task.getResponsible() == null) continue;

                if (task.getResponsible().getDepartment() == null) continue;


                String departmentId = task.getResponsible().getDepartment().getId();

                if (!EnvUtil.getList("APPROVE_DEPARTMENTS").contains(departmentId)) continue;

                Logger.debug("НАРУШЕНИЕ для задачи: " + task.getName());
                Logger.debug("Последняя активность в задаче: " + task.getActivity());
                Logger.debug("ID Задачи: " + task.getId());
                Logger.debug("Ссылка на задачу: " + "https://prodvigaeff.megaplan.ru/task/" + task.getId() + "/card/");
                Logger.debug("Плановые часы: " + task.getPlannedWorkHours());
                Logger.debug("Фактические часы: " + task.getActualWorkHours());
                Logger.debug("Отдел: " + task.getResponsible().getDepartment().getName() + " (ID: " + departmentId + ")");
                Logger.debug("Владелец задачи: " + task.getOwner().getName() + " (ID: " + task.getOwner().getId() + ")");
                Logger.debug("Ответственный за задачу: " + task.getResponsible().getName() + " (ID: " + task.getResponsible().getId() + ")");

                violations.add(task);
            }
        }

        return violations;
    }
}
