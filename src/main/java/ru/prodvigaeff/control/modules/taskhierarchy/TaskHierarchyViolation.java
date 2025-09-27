package ru.prodvigaeff.control.modules.taskhierarchy;

import ru.prodvigaeff.control.model.Task;

public class TaskHierarchyViolation
{
    private final Task parentTask;
    private final Task subtask;
    private final long hoursDifference;

    public TaskHierarchyViolation(Task parentTask, Task subtask, long hoursDifference)
    {
        this.parentTask = parentTask;
        this.subtask = subtask;
        this.hoursDifference = hoursDifference;
    }

    public Task getParentTask() { return parentTask; }
    public Task getSubtask() { return subtask; }
    public long getHoursDifference() { return hoursDifference; }
}