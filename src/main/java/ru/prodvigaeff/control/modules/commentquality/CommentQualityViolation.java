package ru.prodvigaeff.control.modules.commentquality;

import ru.prodvigaeff.control.model.Task;

public class CommentQualityViolation
{
    private final Task task;
    private final Task.TaskComment comment;
    private final AIAnalysisResult aiResult;

    public CommentQualityViolation(Task task, Task.TaskComment comment, AIAnalysisResult aiResult)
    {
        this.task = task;
        this.comment = comment;
        this.aiResult = aiResult;
    }

    public Task getTask() { return task; }
    public Task.TaskComment getComment() { return comment; }
    public AIAnalysisResult getAiResult() { return aiResult; }
}