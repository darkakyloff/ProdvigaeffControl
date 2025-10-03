package ru.prodvigaeff.control.modules.tasktime;

import ru.prodvigaeff.control.megaplan.managers.MegaplanTask;
import ru.prodvigaeff.control.model.Task;
import ru.prodvigaeff.control.modules.worktime.WorkTimeViolation;
import ru.prodvigaeff.control.service.EmailSender;
import ru.prodvigaeff.control.utils.DateUtil;
import ru.prodvigaeff.control.utils.EnvUtil;
import ru.prodvigaeff.control.utils.Logger;
import ru.prodvigaeff.control.utils.StringUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskTimeNotifier
{
    private final EmailSender emailSender;

    public TaskTimeNotifier(EmailSender emailSender)
    {
        this.emailSender = emailSender;
    }

    public void sendNotification (List<Task> violations)
    {
        if (violations.isEmpty()) return;

        int successCount = 0;
        int errorCount = 0;

        for (Task violation : violations)
        {
            try
            {
                sendSingleNotification(violation);
                successCount++;
            }
            catch (Exception e)
            {
                errorCount++;
                Logger.error("Ошибка отправки уведомления: " + e.getMessage());
            }
        }

        Logger.debug("Отправлено успешно: " + successCount + ", ошибок: " + errorCount);
    }

    private void sendSingleNotification(Task violation)
    {
        Task.Employee responsible = violation.getResponsible();
        if (responsible == null || responsible.getId() == null) return;

        Task.Employee owner = violation.getOwner();
        if (owner == null || owner.getId() == null) return;

        if (!StringUtil.isValidCompanyEmail(responsible.getEmail(), "prodvigaeff.ru")) return;

        Map<String, String> data = createEmailData(violation);
        String subject = "Нарушение заполнения рабочих часов";

        emailSender.sendEmail(responsible.getEmail(), subject, "TaskTime.html", data);
    }

    private Map<String, String> createEmailData(Task violation)
    {
        Map<String, String> data = new HashMap<>();

        LocalDateTime now = LocalDateTime.now(DateUtil.MSK_ZONE);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        data.put("CHECK_DATE", now.format(formatter));

        data.put("EMPLOYEE_NAME", violation.getResponsible() != null && violation.getResponsible().getName() != null
                ? violation.getResponsible().getName()
                : "Сотрудник");

        data.put("TASK_NAME", violation.getName() != null ? violation.getName() : "Без названия");
        data.put("TASK_ID", violation.getId());

        String baseUrl = EnvUtil.get("MEGAPLAN_URL", "https://prodvigaeff.megaplan.ru");
        data.put("TASK_URL", baseUrl + "/task/" + violation.getId() + "/card/");

        double plannedHours = violation.getPlannedWorkHours();
        double actualHours = violation.getActualWorkHours();
        double difference = actualHours - plannedHours;
        double percentageDiff = plannedHours > 0 ? (difference / plannedHours * 100) : 0;

        data.put("PLANNED_HOURS", String.format("%.1f", plannedHours));
        data.put("ACTUAL_HOURS", String.format("%.1f", actualHours));
        data.put("HOURS_DIFFERENCE", String.format("%.1f", difference));
        data.put("PERCENTAGE_DIFFERENCE", String.format("%.0f", percentageDiff));

        String departmentName = "Не указан";
        if (violation.getResponsible() != null &&
                violation.getResponsible().getDepartment() != null &&
                violation.getResponsible().getDepartment().getName() != null)
        {
            departmentName = violation.getResponsible().getDepartment().getName();
        }
        data.put("ACTIVITY_DATE", violation.getActivity().format(formatter));

        String ownerName = "Не указан";
        if (violation.getOwner() != null && violation.getOwner().getName() != null)
        {
            ownerName = violation.getOwner().getName();
        }
        data.put("OWNER_NAME", ownerName);

        String responsibleName = "Не указан";
        if (violation.getResponsible() != null && violation.getResponsible().getName() != null)
        {
            responsibleName = violation.getResponsible().getName();
        }
        data.put("RESPONSIBLE_NAME", responsibleName);

        return data;
    }

}
