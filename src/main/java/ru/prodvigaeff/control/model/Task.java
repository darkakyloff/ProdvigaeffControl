package ru.prodvigaeff.control.model;

import java.time.LocalDateTime;
import java.util.List;

public class Task
{
    private String id;
    private String name;
    private String status;
    private Employee owner;
    private Employee responsible;
    private List<TaskComment> comments;
    private List<String> subtaskIds;
    private LocalDateTime timeCreated;
    private LocalDateTime activity;
    private double plannedWorkHours;
    private double actualWorkHours;

    public Task(String id, String name, String status, Employee owner, Employee responsible)
    {
        this.id = id;
        this.name = name;
        this.status = status;
        this.owner = owner;
        this.responsible = responsible;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public Employee getOwner() { return owner; }
    public Employee getResponsible() { return responsible; }
    public List<TaskComment> getComments() { return comments; }
    public List<String> getSubtaskIds() { return subtaskIds; }
    public LocalDateTime getTimeCreated() { return timeCreated; }
    public LocalDateTime getActivity() { return activity; }
    public double getPlannedWorkHours() { return plannedWorkHours; }
    public double getActualWorkHours() { return actualWorkHours; }

    public void setComments(List<TaskComment> comments) { this.comments = comments; }
    public void setSubtaskIds(List<String> subtaskIds) { this.subtaskIds = subtaskIds; }
    public void setTimeCreated(LocalDateTime timeCreated) { this.timeCreated = timeCreated; }
    public void setActivity(LocalDateTime activity) { this.activity = activity; }
    public void setPlannedWorkHours(double plannedWorkHours) { this.plannedWorkHours = plannedWorkHours; }
    public void setActualWorkHours(double actualWorkHours) { this.actualWorkHours = actualWorkHours; }

    public static class TaskComment
    {
        private String id;
        private String content;
        private Employee author;
        private LocalDateTime commentDate;
        private LocalDateTime workDate;
        private double workHours;
        private String taskId;

        public TaskComment(String id, String content, Employee author,
                           LocalDateTime commentDate, LocalDateTime workDate,
                           double workHours, String taskId)
        {
            this.id = id;
            this.content = content;
            this.author = author;
            this.commentDate = commentDate;
            this.workDate = workDate;
            this.workHours = workHours;
            this.taskId = taskId;
        }

        public String getId() { return id; }
        public String getContent() { return content; }
        public Employee getAuthor() { return author; }
        public LocalDateTime getCommentDate() { return commentDate; }
        public LocalDateTime getWorkDate() { return workDate; }
        public double getWorkHours() { return workHours; }
        public String getTaskId() { return taskId; }

        public boolean hasWorkTime()
        {
            return workHours > 0 && workDate != null;
        }
    }

    public static class Employee
    {
        private String id;
        private String name;
        private String email;
        private String position;
        private Department department;

        public Employee(String id, String name, String email, String position)
        {
            this.id = id;
            this.name = name;
            this.email = email;
            this.position = position;
        }

        public Employee(String id, String name, String email, String position, Department department)
        {
            this.id = id;
            this.name = name;
            this.email = email;
            this.position = position;
            this.department = department;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getPosition() { return position; }
        public Department getDepartment() { return department; }

        public void setDepartment(Department department) { this.department = department; }
    }

    public static class Department
    {
        private String id;
        private String name;

        public Department(String id, String name)
        {
            this.id = id;
            this.name = name;
        }

        public String getId() { return id; }
        public String getName() { return name; }
    }
}