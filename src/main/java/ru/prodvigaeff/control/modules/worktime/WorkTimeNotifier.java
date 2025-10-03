package ru.prodvigaeff.control.modules.worktime;

import ru.prodvigaeff.control.megaplan.managers.MegaplanTask;
import ru.prodvigaeff.control.model.Task;
import ru.prodvigaeff.control.service.EmailSender;
import ru.prodvigaeff.control.utils.DateUtil;
import ru.prodvigaeff.control.utils.EnvUtil;
import ru.prodvigaeff.control.utils.Logger;
import ru.prodvigaeff.control.utils.StringUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkTimeNotifier
{
    private final EmailSender emailSender;

    public WorkTimeNotifier(EmailSender emailSender)
    {
        this.emailSender = emailSender;
    }

    public void sendNotifications(List<WorkTimeViolation> violations)
    {
        if (violations.isEmpty())
        {
            Logger.debug("Нет нарушений для отправки уведомлений");
            return;
        }

        Logger.debug("Начинаем отправку " + violations.size() + " уведомлений");

        int successCount = 0;
        int errorCount = 0;

        for (WorkTimeViolation violation : violations)
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

    private void sendSingleNotification(WorkTimeViolation violation)
    {
        Task.Employee author = violation.getComment().getAuthor();
        if (author == null || author.getId() == null) return;

        Task.Employee fullEmployee = MegaplanTask.getEmployeeById(author.getId());
        if (fullEmployee == null || fullEmployee.getEmail() == null) return;

        if (!StringUtil.isValidCompanyEmail(fullEmployee.getEmail(), "prodvigaeff.ru")) return;

        Map<String, String> data = createEmailData(violation, fullEmployee);
        String subject = "Нарушения заполнения рабочих часов " + fullEmployee.getName();

        emailSender.sendEmail(fullEmployee.getEmail(), subject, "WorkData.html", data);
    }

    private Map<String, String> createEmailData(WorkTimeViolation violation, Task.Employee employee)
    {
        Map<String, String> data = new HashMap<>();

        data.put("now_date", DateUtil.today().toString());
        data.put("full_name", employee.getName() != null ? employee.getName() : "Сотрудник");
        data.put("task_id", violation.getTask().getId());
        data.put("time_diff", String.valueOf(violation.getDaysDifference()));
        data.put("comment_data", violation.getComment().getCommentDate().toLocalDate().toString());
        data.put("work_data", violation.getComment().getWorkDate().toLocalDate().toString());
        data.put("hourse", String.format("%.1f", violation.getComment().getWorkHours()));
        data.put("comment_id", violation.getComment().getId());
        data.put("comment_context", StringUtil.isNotEmpty(violation.getComment().getContent()) ? StringUtil.truncate(violation.getComment().getContent(), 200) : "Содержимое отсутствует");

        String baseUrl = EnvUtil.get("MEGAPLAN_URL", "https://prodvigaeff.megaplan.ru");
        data.put("task_url", baseUrl + "/task/" + violation.getTask().getId() + "/card/");

        return data;
    }
}
