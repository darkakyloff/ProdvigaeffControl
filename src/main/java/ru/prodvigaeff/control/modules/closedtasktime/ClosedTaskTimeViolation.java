package ru.prodvigaeff.control.modules.closedtasktime;

import ru.prodvigaeff.control.model.Task;
import java.time.LocalDateTime;

public class ClosedTaskTimeViolation
{
    private final Task task;
    private final Task.TaskComment comment;
    private final Task.Employee violator;
    private final LocalDateTime violationTime;
    private final double hoursAdded;
    private final String taskStatus;

    public ClosedTaskTimeViolation(Task task, Task.TaskComment comment, Task.Employee violator, LocalDateTime violationTime, double hoursAdded, String taskStatus)
    {
        this.task = task;
        this.comment = comment;
        this.violator = violator;
        this.violationTime = violationTime;
        this.hoursAdded = hoursAdded;
        this.taskStatus = taskStatus;
    }

    public Task getTask()
    {
        return task;
    }

    public Task.TaskComment getComment()
    {
        return comment;
    }

    public Task.Employee getViolator()
    {
        return violator;
    }

    public LocalDateTime getViolationTime()
    {
        return violationTime;
    }

    public double getHoursAdded()
    {
        return hoursAdded;
    }

    public String getTaskStatus()
    {
        return taskStatus;
    }

    public String getTaskUrl()
    {
        return "https://prodvigaeff.megaplan.ru/task/" + task.getId() + "/card";
    }

    @Override
    public String toString()
    {
        return "ClosedTaskTimeViolation{" +
                "task=" + task.getName() +
                ", violator=" + (violator != null ? violator.getName() : "Unknown") +
                ", hours=" + hoursAdded +
                ", status=" + taskStatus +
                ", time=" + violationTime +
                '}';
    }
}