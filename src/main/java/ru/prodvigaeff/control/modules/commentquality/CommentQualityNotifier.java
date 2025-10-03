package ru.prodvigaeff.control.modules.commentquality;

import ru.prodvigaeff.control.megaplan.managers.MegaplanTask;
import ru.prodvigaeff.control.model.Task;
import ru.prodvigaeff.control.service.EmailSender;
import ru.prodvigaeff.control.utils.EnvUtil;
import ru.prodvigaeff.control.utils.Logger;
import ru.prodvigaeff.control.utils.StringUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CommentQualityNotifier
{
    private final EmailSender emailSender;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public CommentQualityNotifier(EmailSender emailSender)
    {
        this.emailSender = emailSender;
    }

    public void sendNotifications(List<CommentQualityViolation> violations)
    {
        if (violations.isEmpty())
        {
            Logger.debug("Нет нарушений для отправки уведомлений");
            return;
        }

        Logger.debug("Начинаем отправку " + violations.size() + " уведомлений");

        int successCount = 0;
        int errorCount = 0;

        for (CommentQualityViolation violation : violations)
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

    private void sendSingleNotification(CommentQualityViolation violation)
    {
        Task.Employee author = violation.getComment().getAuthor();
        if (author == null || author.getId() == null) return;

        Task.Employee fullEmployee = MegaplanTask.getEmployeeById(author.getId());
        if (fullEmployee == null || fullEmployee.getEmail() == null) return;

        if (!StringUtil.isValidCompanyEmail(fullEmployee.getEmail(), "prodvigaeff.ru")) return;

        Map<String, String> data = createEmailData(violation, fullEmployee);
        String subject = "Низкое качество описания работы - " + fullEmployee.getName();

        emailSender.sendEmail(fullEmployee.getEmail(), subject, "CommentQualityEmail.html", data);
        Logger.debug("Email уведомление отправлено: " + fullEmployee.getEmail());
    }

    private Map<String, String> createEmailData(CommentQualityViolation violation, Task.Employee employee)
    {
        Map<String, String> data = new HashMap<>();

        data.put("CHECK_DATE", LocalDateTime.now().format(DATE_FORMATTER));
        data.put("EMPLOYEE_NAME", employee.getName() != null ? employee.getName() : "Сотрудник");
        data.put("TASK_ID", violation.getTask().getId());
        data.put("TASK_NAME", cleanTaskName(violation.getTask().getName()));
        data.put("COMMENT_DATE", violation.getComment().getCommentDate().format(DATE_FORMATTER));

        data.put("WORK_HOURS", String.format(Locale.US, "%.1f", violation.getComment().getWorkHours()));

        data.put("COMMENT_TEXT", StringUtil.isNotEmpty(violation.getComment().getContent())
                ? StringUtil.truncate(violation.getComment().getContent(), 500)
                : "Содержимое отсутствует");

        AIAnalysisResult aiResult = violation.getAiResult();
        data.put("DETAIL_SCORE", String.valueOf(aiResult.getDetailScore()));
        data.put("REALISM_SCORE", String.valueOf(aiResult.getRealismScore()));
        data.put("CONCRETE_SCORE", String.valueOf(aiResult.getConcreteScore()));

        data.put("TOTAL_SCORE", String.format(Locale.US, "%.1f", aiResult.getTotalScore()));

        data.put("AI_REASON", aiResult.getReason());

        String baseUrl = EnvUtil.get("MEGAPLAN_URL", "https://prodvigaeff.megaplan.ru");
        data.put("TASK_URL", baseUrl + "/task/" + violation.getTask().getId() + "/card/");

        return data;
    }

    private String cleanTaskName(String taskName)
    {
        if (taskName == null) return "Без названия";
        return taskName.replace("prodvigaeff.ruот", "prodvigaeff.ru от ").trim();
    }
}