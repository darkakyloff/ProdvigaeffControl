package ru.prodvigaeff.control.modules.worktime;

import ru.prodvigaeff.control.model.Task;

public class WorkTimeViolation
{
    private final Task task;
    private final Task.TaskComment comment;
    private final int daysDifference;

    public WorkTimeViolation(Task task, Task.TaskComment comment, int daysDifference)
    {
        this.task = task;
        this.comment = comment;
        this.daysDifference = daysDifference;
    }

    public Task getTask() { return task; }
    public Task.TaskComment getComment() { return comment; }
    public int getDaysDifference() { return daysDifference; }
}