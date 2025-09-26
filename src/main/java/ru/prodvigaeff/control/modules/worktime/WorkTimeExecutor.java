package ru.prodvigaeff.control.modules.worktime;

import ru.prodvigaeff.control.megaplan.managers.MegaplanTask;
import ru.prodvigaeff.control.model.Task;
import ru.prodvigaeff.control.service.EmailService;
import ru.prodvigaeff.control.utils.DateUtil;
import ru.prodvigaeff.control.utils.EnvUtil;
import ru.prodvigaeff.control.utils.Logger;
import ru.prodvigaeff.control.utils.StringUtil;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class WorkTimeExecutor
{
    private static final String TEMPLATE_PATH = "src/main/resources/templates/WorkData.html";

    public static HashMap<Task, Task.TaskComment> getViolation()
    {
        LocalDateTime now = LocalDateTime.now();
        return getViolation(now);
    }

    public static HashMap<Task, Task.TaskComment> getViolation(LocalDateTime currentTime)
    {
        Set<Task> tasks = MegaplanTask.getAllTasks();
        HashMap<Task, Task.TaskComment> violations = new HashMap<>();

        Logger.info("Начинаем проверку нарушений рабочего времени");

        ZonedDateTime currentMsk = currentTime.atZone(DateUtil.MSK_ZONE);
        LocalDateTime checkStart = currentMsk.minusHours(24).toLocalDateTime();
        LocalDateTime checkEnd = currentMsk.toLocalDateTime();

        Logger.info("Проверяем период с " + checkStart + " по " + checkEnd + " (последние 24 часа)");

        int processedTasks = 0;
        int totalTasks = tasks.size();

        for (Task task : tasks)
        {
            processedTasks++;
            Logger.progress("Проверка задач", processedTasks, totalTasks);

            if (task == null)
            {
                Logger.debug("Найдена null задача, пропускаем");
                continue;
            }

            if (task.getComments() == null || task.getComments().isEmpty())
            {
                Logger.debug("Задача " + task.getId(), "Нет комментариев");
                continue;
            }

            Logger.debug("Задача " + task.getId(), "Комментариев: " + task.getComments().size());

            for (Task.TaskComment comment : task.getComments())
            {
                if (comment == null)
                {
                    Logger.debug("Найден null комментарий в задаче " + task.getId());
                    continue;
                }

                ViolationCheckResult result = checkCommentForViolation(comment, checkStart, checkEnd);

                if (result.isViolation)
                {
                    Logger.warn("Найдено нарушение в задаче " + task.getId() + ", комментарий " + comment.getId() + ": " + result.violationReason);
                    violations.put(task, comment);
                }
            }
        }

        Logger.info("Проверка завершена. Найдено нарушений: " + violations.size());
        return violations;
    }

    private static ViolationCheckResult checkCommentForViolation(Task.TaskComment comment,
                                                                 LocalDateTime checkStart,
                                                                 LocalDateTime checkEnd)
    {
        ViolationCheckResult result = new ViolationCheckResult();

        if (!comment.hasWorkTime())
        {
            Logger.debug("Комментарий " + comment.getId(), "Нет рабочего времени");
            return result;
        }

        if (comment.getWorkHours() <= 0)
        {
            Logger.debug("Комментарий " + comment.getId(), "Часы <= 0: " + comment.getWorkHours());
            return result;
        }

        LocalDateTime commentDate = comment.getCommentDate();
        LocalDateTime workDate = comment.getWorkDate();

        if (commentDate == null || workDate == null)
        {
            Logger.debug("Комментарий " + comment.getId(), "Даты null");
            return result;
        }

        Logger.debug("Комментарий " + comment.getId(),
                "Дата комментария: " + commentDate + ", дата работы: " + workDate);

        if (commentDate.isBefore(checkStart) || commentDate.isAfter(checkEnd))
        {
            Logger.debug("Комментарий " + comment.getId(), "Не попадает в проверяемый период последних 24 часов");
            return result;
        }

        if (!DateUtil.isSameDate(commentDate, workDate))
        {
            int daysDifference = Math.abs(DateUtil.daysDifference(commentDate.toLocalDate(), workDate.toLocalDate()));
            result.isViolation = true;
            result.violationReason = "Разница между датой комментария и датой работы: " + daysDifference + " дней";
        }

        return result;
    }

    public static void sendEmailMessages(HashMap<Task, Task.TaskComment> violations)
    {
        if (violations.isEmpty())
        {
            Logger.info("Нет нарушений для отправки уведомлений");
            return;
        }

        Logger.info("Начинаем отправку " + violations.size() + " уведомлений");

        int successCount = 0;
        int errorCount = 0;

        for (Map.Entry<Task, Task.TaskComment> entry : violations.entrySet())
        {
            try
            {
                boolean sent = sendSingleEmailNotification(entry.getKey(), entry.getValue());

                if (sent) successCount++;
                else errorCount++;

            }
            catch (Exception e)
            {
                errorCount++;
                Logger.error("Неожиданная ошибка при отправке уведомления: " + e.getMessage());
            }
        }

        Logger.result("Отправка уведомлений", successCount, System.currentTimeMillis());
        if (errorCount > 0)
        {
            Logger.warn("Не удалось отправить " + errorCount + " уведомлений");
        }
    }

    private static boolean sendSingleEmailNotification(Task task, Task.TaskComment comment)
    {
        if (comment.getAuthor() == null || StringUtil.isEmpty(comment.getAuthor().getId()))
        {
            Logger.warn("Нет ID автора для комментария " + comment.getId());
            return false;
        }

        String authorId = comment.getAuthor().getId();
        Task.Employee fullEmployee = MegaplanTask.getEmployeeById(authorId);

        if (fullEmployee == null || "не найдено".equals(fullEmployee.getEmail()))
        {
            Logger.warn("Не удалось получить email для сотрудника " + authorId);
            return false;
        }

        if (!StringUtil.isValidCompanyEmail(fullEmployee.getEmail(), "prodvigaeff.ru"))
        {
            Logger.warn("Некорректный email сотрудника " + authorId + ": " +
                    StringUtil.maskEmail(fullEmployee.getEmail()));
            return false;
        }

        String employeeEmail = fullEmployee.getEmail();
        String employeeName = StringUtil.isNotEmpty(fullEmployee.getName()) ? fullEmployee.getName() : "Сотрудник";

        Map<String, String> placeholders = createEmailPlaceholders(task, comment, fullEmployee);
        String subject = "Нарушения заполнения рабочих часов " + employeeName;

        try
        {
            EmailService.sendEmail(employeeEmail, subject, TEMPLATE_PATH, placeholders);
            Logger.success("Отправлено уведомление сотруднику " + employeeName + " (" + StringUtil.maskEmail(employeeEmail) + ")");
            return true;
        }
        catch (Exception e)
        {
            Logger.error("Ошибка отправки email для " + employeeName + ": " + e.getMessage());
            return false;
        }
    }

    private static Map<String, String> createEmailPlaceholders(Task task, Task.TaskComment comment, Task.Employee employee)
    {
        int timeDiff = Math.abs(DateUtil.daysDifference(
                comment.getCommentDate().toLocalDate(),
                comment.getWorkDate().toLocalDate()
        ));

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("now_date", DateUtil.today().toString());
        placeholders.put("full_name", employee.getName() != null ? employee.getName() : "Сотрудник");
        placeholders.put("task_id", task.getId() != null ? task.getId() : "неизвестно");
        placeholders.put("time_diff", String.valueOf(timeDiff));
        placeholders.put("comment_data", comment.getCommentDate().toLocalDate().toString());
        placeholders.put("work_data", comment.getWorkDate().toLocalDate().toString());
        placeholders.put("hourse", String.format("%.1f", comment.getWorkHours()));
        placeholders.put("comment_id", comment.getId() != null ? comment.getId() : "неизвестно");
        placeholders.put("comment_context", StringUtil.isNotEmpty(comment.getContent()) ? StringUtil.truncate(comment.getContent(), 200) : "Содержимое отсутствует");

        String baseUrl = EnvUtil.get("MEGAPLAN_URL", "https://prodvigaeff.megaplan.ru");
        placeholders.put("task_url", baseUrl + "/task/" + task.getId() + "/card/");

        return placeholders;
    }

    private static class ViolationCheckResult
    {
        boolean isViolation = false;
        String violationReason = "";
    }
}