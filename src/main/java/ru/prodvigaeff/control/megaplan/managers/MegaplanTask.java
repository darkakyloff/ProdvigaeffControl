package ru.prodvigaeff.control.megaplan.managers;

import ru.prodvigaeff.control.http.HttpBuilder;
import ru.prodvigaeff.control.http.HttpResponse;
import ru.prodvigaeff.control.model.Task;
import ru.prodvigaeff.control.utils.EnvUtil;
import ru.prodvigaeff.control.utils.JsonUtil;
import ru.prodvigaeff.control.utils.Logger;
import ru.prodvigaeff.control.utils.StringUtil;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class MegaplanTask
{
    private static final String MEGAPLAN_API_KEY = EnvUtil.get("MEGAPLAN_API_KEY");
    private static final int BATCH_SIZE = EnvUtil.getInt("MEGAPLAN_BATCH_SIZE", 20);
    private static final int THREAD_POOL_SIZE = EnvUtil.getInt("MEGAPLAN_THREAD_POOL_SIZE", 5);
    private static final ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    private static final Map<String, Task.Employee> employeeCache = new ConcurrentHashMap<>();

    public static Set<Task> getAllTasks()
    {
        Set<Task> allTasks = new HashSet<>();
        String pageAfter = null;
        boolean hasMore = true;
        int pageCount = 0;

        while (hasMore)
        {
            pageCount++;
            String url = buildTaskUrl(pageAfter);

            HttpResponse response = HttpBuilder
                    .get(url)
                    .auth(MEGAPLAN_API_KEY)
                    .execute();

            if (!response.isSuccess())
            {
                Logger.error("Ошибка получения задач на странице " + pageCount + ": " + response.getStatusCode());
                break;
            }

            Map<String, Object> jsonResponse = JsonUtil.fromJson(response.getBody(), Map.class);
            Map<String, Object> meta = (Map) jsonResponse.get("meta");
            Map<String, Object> pagination = (Map) meta.get("pagination");

            hasMore = (Boolean) pagination.get("hasMoreNext");
            List<Map<String, Object>> tasksData = (List) jsonResponse.get("data");

            List<Task> pageTasks = new ArrayList<>();
            for (Map<String, Object> taskData : tasksData)
            {
                Task task = parseTask(taskData);
                if (task != null)
                {
                    pageTasks.add(task);
                    pageAfter = task.getId();
                }
            }

            loadCommentsForTasks(pageTasks);
            allTasks.addAll(pageTasks);

            Logger.info("Страница " + pageCount + ": получено " + tasksData.size() + " задач, всего: " + allTasks.size());
        }

        Logger.success("Всего получено задач: " + allTasks.size() + " за " + pageCount + " страниц");
        return allTasks;
    }

    private static void loadCommentsForTasks(List<Task> tasks)
    {
        if (tasks.isEmpty()) return;

        Logger.info("Загружаем комментарии для " + tasks.size() + " задач...");

        List<List<Task>> batches = createBatches(tasks, BATCH_SIZE);

        List<CompletableFuture<Void>> futures = batches.stream()
                .map(batch -> CompletableFuture.runAsync(() -> processBatch(batch), executor))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        Logger.success("Комментарии загружены для всех задач");
    }

    private static void processBatch(List<Task> batch)
    {
        for (Task task : batch)
        {
            try
            {
                List<Task.TaskComment> comments = getTaskComments(task.getId());
                task.setComments(comments);

                Thread.sleep(10);
            }
            catch (Exception e)
            {
                Logger.error("Ошибка получения комментариев для задачи " + task.getId() + ": " + e.getMessage());
                task.setComments(new ArrayList<>());
            }
        }
    }

    private static <T> List<List<T>> createBatches(List<T> items, int batchSize)
    {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < items.size(); i += batchSize)
        {
            int end = Math.min(i + batchSize, items.size());
            batches.add(new ArrayList<>(items.subList(i, end)));
        }
        return batches;
    }

    private static String buildTaskUrl(String pageAfter)
    {
        String baseUrl = EnvUtil.get("MEGAPLAN_URL", "https://prodvigaeff.megaplan.ru");
        String endpoint = baseUrl + "/api/v3/task?";

        String jsonParam;

        if (pageAfter != null) jsonParam = "{\"limit\":100,\"pageAfter\":{\"contentType\":\"Task\",\"id\":\"" + pageAfter + "\"},\"fields\":[\"id\",\"name\",\"status\",\"owner\",\"responsible\",\"subTasks\",\"timeCreated\",\"activity\"]}";
        else jsonParam = "{\"limit\":100,\"fields\":[\"id\",\"name\",\"status\",\"owner\",\"responsible\",\"subTasks\",\"timeCreated\",\"activity\"]}";

        return endpoint + StringUtil.urlEncode(jsonParam);
    }

    public static List<Task.TaskComment> getTaskComments(String taskId)
    {
        if (StringUtil.isEmpty(taskId))
        {
            Logger.warn("Пустой ID задачи для получения комментариев");
            return new ArrayList<>();
        }

        String baseUrl = EnvUtil.get("MEGAPLAN_URL", "https://prodvigaeff.megaplan.ru");
        String endpoint = baseUrl + "/api/v3/task/" + taskId + "/comments";

        HttpResponse response = HttpBuilder
                .get(endpoint)
                .auth(MEGAPLAN_API_KEY)
                .execute();

        if (!response.isSuccess())
        {
            Logger.error("Ошибка получения комментариев для задачи " + taskId + ": " + response.getStatusCode());
            return new ArrayList<>();
        }

        try
        {
            Map<String, Object> jsonResponse = JsonUtil.fromJson(response.getBody(), Map.class);
            List<Map<String, Object>> commentsData = (List) jsonResponse.get("data");

            if (commentsData == null) return new ArrayList<>();

            List<Task.TaskComment> comments = new ArrayList<>();
            for (Map<String, Object> commentData : commentsData)
            {
                Task.TaskComment comment = parseComment(commentData, taskId);
                if (comment != null) comments.add(comment);
            }

            return comments;
        }
        catch (Exception e)
        {
            Logger.error("Ошибка парсинга комментариев для задачи " + taskId + ": " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private static Task parseTask(Map<String, Object> taskData)
    {
        try
        {
            String id = (String) taskData.get("id");
            String name = (String) taskData.get("name");
            String status = (String) taskData.get("status");

            if (StringUtil.isEmpty(id))
            {
                Logger.warn("Задача без ID, пропускаем");
                return null;
            }

            Task.Employee owner = parseEmployee((Map) taskData.get("owner"));
            Task.Employee responsible = parseEmployee((Map) taskData.get("responsible"));
            LocalDateTime timeCreated = parseDateTime((Map) taskData.get("timeCreated"));
            LocalDateTime activity = parseDateTime((Map) taskData.get("activity"));

            Task task = new Task(id, name, status, owner, responsible);
            task.setTimeCreated(timeCreated);
            task.setActivity(activity);

            if (taskData.containsKey("subTasks")) {
                List<Map<String, Object>> subTasks = (List<Map<String, Object>>) taskData.get("subTasks");

                List<String> subtaskIds = subTasks.stream()
                        .map(sub -> (String) sub.get("id"))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                task.setSubtaskIds(subtaskIds);
            }

            return task;
        }
        catch (Exception e)
        {
            Logger.error("Ошибка парсинга задачи: " + e.getMessage());
            return null;
        }
    }

    private static Task.TaskComment parseComment(Map<String, Object> commentData, String taskId)
    {
        try
        {
            String id = (String) commentData.get("id");
            String rawContent = (String) commentData.get("content");
            String content = StringUtil.cleanHtml(rawContent);

            Map<String, Object> ownerData = (Map) commentData.get("owner");
            String authorId = ownerData != null ? (String) ownerData.get("id") : null;

            Task.Employee author = authorId != null ? new Task.Employee(authorId, null, null, null) : null;

            Map<String, Object> timeCreated = (Map) commentData.get("timeCreated");
            Map<String, Object> workDate = (Map) commentData.get("workDate");
            Map<String, Object> workTime = (Map) commentData.get("workTime");

            LocalDateTime commentDate = parseDateTime(timeCreated);
            LocalDateTime workDateTime = parseDateTime(workDate);
            double workHours = parseWorkHours(workTime);

            return new Task.TaskComment(id, content, author, commentDate, workDateTime, workHours, taskId);
        }
        catch (Exception e)
        {
            Logger.error("Ошибка парсинга комментария: " + e.getMessage());
            return null;
        }
    }

    private static Task.Employee parseEmployee(Map<String, Object> empData)
    {
        if (empData == null) return null;

        try
        {

            String id = (String) empData.get("id");
            String name = (String) empData.get("name");
            String position = (String) empData.get("position");

            String email = findEmail((List) empData.get("contactInfo"));

            return new Task.Employee(id, name, email, position);
        }
        catch (Exception e)
        {
            Logger.error("Ошибка парсинга сотрудника: " + e.getMessage());
            return null;
        }
    }

    private static LocalDateTime parseDateTime(Map<String, Object> dateTimeData)
    {
        if (dateTimeData == null) return null;

        try
        {
            String value = (String) dateTimeData.get("value");
            if (value == null) return null;

            return ru.prodvigaeff.control.utils.DateUtil.parseIsoToMsk(value);
        }
        catch (Exception e)
        {
            Logger.error("Ошибка парсинга даты: " + e.getMessage());
            return null;
        }
    }

    private static double parseWorkHours(Map<String, Object> workTimeData)
    {
        if (workTimeData == null) return 0.0;

        try
        {
            Object value = workTimeData.get("value");
            if (value == null) return 0.0;

            if (value instanceof Integer) return ((Integer) value) / 3600.0;
            if (value instanceof Double) return ((Double) value) / 3600.0;

            return 0.0;
        }
        catch (Exception e)
        {
            Logger.error("Ошибка парсинга рабочих часов: " + e.getMessage());
            return 0.0;
        }
    }

    private static String findEmail(List<Map<String, Object>> contactInfo)
    {
        if (contactInfo == null) return "не найдено";

        try
        {
            return contactInfo.stream()
                    .filter(contact -> "email".equals(contact.get("type")))
                    .map(contact -> (String) contact.get("value"))
                    .filter(StringUtil::isNotEmpty)
                    .filter(email -> StringUtil.isValidCompanyEmail(email, "prodvigaeff.ru"))
                    .findFirst()
                    .orElse("не найдено");
        }
        catch (Exception e)
        {
            Logger.error("Ошибка поиска email в контактной информации: " + e.getMessage());
            return "не найдено";
        }
    }

    public static Task.Employee getEmployeeById(String employeeId)
    {
        if (StringUtil.isEmpty(employeeId))
        {
            Logger.warn("Пустой ID сотрудника");
            return null;
        }

        if (employeeCache.containsKey(employeeId)) return employeeCache.get(employeeId);

        String baseUrl = EnvUtil.get("MEGAPLAN_URL", "https://prodvigaeff.megaplan.ru");
        String endpoint = baseUrl + "/api/v3/employee/" + employeeId;

        HttpResponse response = HttpBuilder
                .get(endpoint)
                .auth(MEGAPLAN_API_KEY)
                .execute();

        if (!response.isSuccess())
        {
            Logger.error("Ошибка получения сотрудника " + employeeId + ": " + response.getStatusCode());
            return null;
        }

        try
        {
            Map<String, Object> jsonResponse = JsonUtil.fromJson(response.getBody(), Map.class);
            Map<String, Object> employeeData = (Map) jsonResponse.get("data");

            Task.Employee employee = employeeData != null ? parseEmployee(employeeData) : null;

            if (employee != null) employeeCache.put(employeeId, employee);

            return employee;
        }
        catch (Exception e)
        {
            Logger.error("Ошибка парсинга данных сотрудника " + employeeId + ": " + e.getMessage());
            return null;
        }
    }

    public static void clearEmployeeCache()
    {
        employeeCache.clear();
        Logger.info("Кеш сотрудников очищен");
    }

    public static void shutdown()
    {
        executor.shutdown();
        try
        {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS))
            {
                executor.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        Logger.info("MegaplanTask executor остановлен");
    }

    public static Task getTaskById(String taskId)
    {
        if (StringUtil.isEmpty(taskId))
        {
            Logger.warn("Пустой ID задачи");
            return null;
        }

        String baseUrl = EnvUtil.get("MEGAPLAN_URL", "https://prodvigaeff.megaplan.ru");

        String jsonParam = "{\"fields\":[\"id\",\"name\",\"status\",\"owner\",\"responsible\",\"subTasks\",\"timeCreated\",\"activity\"]}";
        String endpoint = baseUrl + "/api/v3/task/" + taskId + "?" + StringUtil.urlEncode(jsonParam);

        HttpResponse response = HttpBuilder
                .get(endpoint)
                .auth(MEGAPLAN_API_KEY)
                .execute();

        if (!response.isSuccess())
        {
            Logger.error("Ошибка получения задачи " + taskId + ": " + response.getStatusCode());
            return null;
        }

        try
        {
            Map<String, Object> jsonResponse = JsonUtil.fromJson(response.getBody(), Map.class);
            Map<String, Object> taskData = (Map) jsonResponse.get("data");

            return taskData != null ? parseTask(taskData) : null;
        }
        catch (Exception e)
        {
            Logger.error("Ошибка парсинга данных задачи " + taskId + ": " + e.getMessage());
            return null;
        }
    }

}