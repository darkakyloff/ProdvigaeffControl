package ru.prodvigaeff.control.modules.taskhierarchy;

import ru.prodvigaeff.control.megaplan.managers.MegaplanTask;
import ru.prodvigaeff.control.model.Task;
import ru.prodvigaeff.control.service.EmailSender;
import ru.prodvigaeff.control.utils.EnvUtil;
import ru.prodvigaeff.control.utils.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskHierarchyNotifier
{
    private final EmailSender emailSender;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public TaskHierarchyNotifier(EmailSender emailSender)
    {
        this.emailSender = emailSender;
    }

    public void sendNotifications(List<TaskHierarchyViolation> violations)
    {
        if (violations.isEmpty()) return;

        String adminEmails = EnvUtil.get("EMAIL_ADMIN");
        if (adminEmails == null || adminEmails.trim().isEmpty())
        {
            Logger.error("EMAIL_ADMIN не настроен");
            return;
        }

        String[] emails = adminEmails.split(",");
        if (emails.length == 0) return;

        String primaryAdmin = emails[0].trim();
        String subject = "Нарушения дат создания задач - " + LocalDateTime.now().format(DATE_FORMATTER);
        Map<String, String> data = createEmailData(violations);

        try
        {
            emailSender.sendEmail(primaryAdmin, subject, "TaskViolationEmail.html", data);
            Logger.success("Email уведомление о нарушениях дат отправлено");
        }
        catch (Exception e)
        {
            Logger.error("Ошибка отправки email уведомления: " + e.getMessage());
        }
    }

    private Map<String, String> createEmailData(List<TaskHierarchyViolation> violations)
    {
        Map<String, String> data = new HashMap<>();
        data.put("CHECK_DATE", LocalDateTime.now().format(DATE_FORMATTER));
        data.put("VIOLATIONS_COUNT", String.valueOf(violations.size()));
        data.put("MAX_HOURS", "12");

        StringBuilder violationsList = new StringBuilder();
        for (TaskHierarchyViolation violation : violations)
        {
            violationsList.append(generateViolationCard(violation));
        }

        data.put("VIOLATIONS_LIST", violationsList.toString());
        return data;
    }

    private String generateViolationCard(TaskHierarchyViolation violation)
    {
        String taskUrl = generateTaskUrl(violation.getParentTask().getId());
        String taskName = cleanTaskName(violation.getParentTask().getName());

        return "<div class=\"violation-card\">" +
               "<div class=\"violation-header\">" +
               "<div class=\"task-name\">" + taskName + "</div>" +
               "<a href=\"" + taskUrl + "\" class=\"task-link\">Перейти к задаче</a>" +
               "</div>" +
               "<div class=\"violation-details\">" +
               createDetailItem("ID родительской задачи", violation.getParentTask().getId()) +
               createDetailItem("ID подзадачи", violation.getSubtask().getId()) +
               createDetailItem("Дата создания", formatDateRange(violation)) +
               createDetailItem("Владелец", getEmployeeName(violation.getParentTask().getOwner())) +
               createDetailItem("Ответственный", getEmployeeName(violation.getParentTask().getResponsible())) +
               "</div>" +
               "</div>";
    }

    private String createDetailItem(String label, String value)
    {
        return "<div class=\"detail-item\">" +
               "<div class=\"detail-label\">" + label + "</div>" +
               "<div class=\"detail-value\">" + value + "</div>" +
               "</div>";
    }

    private String formatDateRange(TaskHierarchyViolation violation)
    {
        return violation.getParentTask().getTimeCreated().format(DATE_FORMATTER) +
               " → " +
               violation.getSubtask().getTimeCreated().format(DATE_FORMATTER);
    }

    private String generateTaskUrl(String taskId)
    {
        String baseUrl = EnvUtil.get("MEGAPLAN_URL", "https://prodvigaeff.megaplan.ru");
        return baseUrl + "/task/" + taskId + "/card/";
    }

    private String cleanTaskName(String taskName)
    {
        if (taskName == null) return "Без названия";
        return taskName.replace("prodvigaeff.ruот", "prodvigaeff.ru от ").trim();
    }

    private String getEmployeeName(Task.Employee employee)
    {
        if (employee == null || employee.getId() == null) return "Не указан";

        Task.Employee fullEmployee = MegaplanTask.getEmployeeById(employee.getId());
        if (fullEmployee != null && fullEmployee.getName() != null) return fullEmployee.getName();

        return "Не указан";
    }
}