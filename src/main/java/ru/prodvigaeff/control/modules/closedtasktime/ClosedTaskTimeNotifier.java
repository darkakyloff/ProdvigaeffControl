package ru.prodvigaeff.control.modules.closedtasktime;

import ru.prodvigaeff.control.model.Task;
import ru.prodvigaeff.control.service.EmailSender;
import ru.prodvigaeff.control.utils.DateUtil;
import ru.prodvigaeff.control.utils.EnvUtil;
import ru.prodvigaeff.control.utils.Logger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class ClosedTaskTimeNotifier
{
    private final EmailSender emailSender;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    public ClosedTaskTimeNotifier(EmailSender emailSender)
    {
        this.emailSender = emailSender;
    }

    public void sendNotifications(List<ClosedTaskTimeViolation> violations)
    {
        if (violations == null || violations.isEmpty())
        {
            Logger.debug("Нет нарушений для отправки");
            return;
        }

        Map<String, List<ClosedTaskTimeViolation>> violationsByEmployee = violations.stream()
                .collect(Collectors.groupingBy(v ->
                        v.getViolator() != null ? v.getViolator().getId() : "unknown"
                ));

        Logger.info("Отправляем уведомления для " + violationsByEmployee.size() + " сотрудников");

        for (Map.Entry<String, List<ClosedTaskTimeViolation>> entry : violationsByEmployee.entrySet())
        {
            String employeeId = entry.getKey();
            List<ClosedTaskTimeViolation> employeeViolations = entry.getValue();

            try
            {
                sendEmailForEmployee(employeeViolations);
                Logger.debug("Email отправлен для сотрудника: " + employeeId +
                        " (нарушений: " + employeeViolations.size() + ")");
            }
            catch (Exception e)
            {
                Logger.error("Ошибка отправки email для сотрудника " + employeeId + ": " + e.getMessage());
            }
        }

        Logger.info("Все уведомления отправлены");
    }

    private void sendEmailForEmployee(List<ClosedTaskTimeViolation> violations)
    {
        if (violations.isEmpty()) return;

        ClosedTaskTimeViolation firstViolation = violations.get(0);
        Task.Employee violator = firstViolation.getViolator();

        String employeeName = violator != null ? violator.getName() : "Неизвестный сотрудник";
        String employeeEmail = violator != null ? violator.getEmail() : null;

        if (employeeEmail == null || "не найдено".equals(employeeEmail))
        {
            Logger.warn("У сотрудника " + employeeName + " не найден email, отправляем только руководителю");
        }

        String subject = "Учет времени в закрытой задаче";

        Map<String, String> templateData = new HashMap<>();
        templateData.put("EMPLOYEE_NAME", employeeName);
        templateData.put("VIOLATIONS_COUNT", String.valueOf(violations.size()));
        templateData.put("VIOLATIONS_LIST", buildViolationsList(violations));
        templateData.put("CURRENT_DATE", LocalDateTime.now(DateUtil.MSK_ZONE).format(DATE_FORMATTER));

        emailSender.sendEmail(employeeEmail, subject, "ClosedTaskTimeEmail.html", templateData);
    }

    private String buildViolationsList(List<ClosedTaskTimeViolation> violations)
    {
        StringBuilder html = new StringBuilder();

        for (ClosedTaskTimeViolation violation : violations)
        {
            html.append("<div class=\"violation-card\">");

            html.append("<div class=\"violation-header\">");
            html.append("<div class=\"task-name\">");
            html.append("<a href=\"").append(violation.getTaskUrl()).append("\" class=\"task-link\">");
            html.append(escapeHtml(violation.getTask().getName()));
            html.append("</a>");
            html.append("</div>");
            html.append("<div class=\"status-badge\">").append(translateStatus(violation.getTaskStatus())).append("</div>");
            html.append("</div>");

            html.append("<div class=\"violation-details\">");

            html.append("<div class=\"detail-item\">");
            html.append("<div class=\"detail-label\">ID Задачи</div>");
            html.append("<div class=\"detail-value\">").append(violation.getTask().getId()).append("</div>");
            html.append("</div>");

            html.append("<div class=\"detail-item\">");
            html.append("<div class=\"detail-label\">Дата добавления времени</div>");
            html.append("<div class=\"detail-value\">").append(violation.getViolationTime().format(DATE_FORMATTER)).append("</div>");
            html.append("</div>");

            html.append("<div class=\"detail-item\">");
            html.append("<div class=\"detail-label\">Количество часов</div>");
            html.append("<div class=\"detail-value\">").append(String.format("%.1f", violation.getHoursAdded())).append(" ч.</div>");
            html.append("</div>");

            html.append("<div class=\"detail-item\">");
            html.append("<div class=\"detail-label\">Статус задачи</div>");
            html.append("<div class=\"detail-value\">").append(translateStatus(violation.getTaskStatus())).append("</div>");
            html.append("</div>");

            html.append("</div>");
            html.append("</div>");
        }

        return html.toString();
    }

    private String translateStatus(String status)
    {
        if (status == null) return "Неизвестно";

        return switch (status)
        {
            case "completed" -> "Завершена";
            case "done" -> "Выполнена";
            case "accepted" -> "Принята";
            default -> status;
        };
    }

    private String escapeHtml(String text)
    {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}