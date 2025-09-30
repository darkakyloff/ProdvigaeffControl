package ru.prodvigaeff.control.modules.commentquality;

import ru.prodvigaeff.control.http.HttpBuilder;
import ru.prodvigaeff.control.http.HttpResponse;
import ru.prodvigaeff.control.megaplan.managers.MegaplanTask;
import ru.prodvigaeff.control.model.Task;
import ru.prodvigaeff.control.utils.EnvUtil;
import ru.prodvigaeff.control.utils.JsonUtil;
import ru.prodvigaeff.control.utils.Logger;
import ru.prodvigaeff.control.utils.StringUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AIAnalysisService
{
    private static final String AI_API_URL = EnvUtil.get("AI_API_URL", "https://text.pollinations.ai/openai");

    public AIAnalysisResult analyze(String commentText, double workHours, Task.Employee author, String taskName)
    {
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries)
        {
            try
            {
                commentText = fixEncoding(commentText);
                taskName = fixEncoding(taskName);

                String position = getEmployeePosition(author);
                String prompt = buildPrompt(commentText, workHours, position, taskName);

                Logger.debug("Анализируем комментарий (попытка " + (attempt + 1) + "): " +
                        StringUtil.truncate(commentText, 50) + "...");

                String response = sendAIRequest(prompt);
                AIAnalysisResult result = parseAIResponse(response);

                Logger.debug("Оценка ИИ: " + result.getTotalScore() + "/10 - " + result.getVerdict());
                Logger.debug("  Конкретика: " + result.getConcreteScore() + "/10");
                Logger.debug("  Реалистичность: " + result.getRealismScore() + "/10");

                return result;
            }
            catch (ContentFilterException e)
            {
                Logger.warn("Комментарий заблокирован content filter. Пропускаем.");
                return null;
            }
            catch (Exception e)
            {
                attempt++;
                if (attempt < maxRetries)
                {
                    Logger.warn("Ошибка AI анализа (попытка " + attempt + "/" + maxRetries + "): " +
                            e.getMessage() + ". Повторяем через 2 секунды...");
                    try { Thread.sleep(2000); } catch (InterruptedException ie) {}
                }
                else
                {
                    Logger.error("Ошибка AI анализа после " + maxRetries + " попыток: " + e.getMessage());
                    return null;
                }
            }
        }

        return null;
    }

    private String getEmployeePosition(Task.Employee author)
    {
        if (author == null || author.getId() == null) return "Не указана";

        Task.Employee fullEmployee = MegaplanTask.getEmployeeById(author.getId());
        if (fullEmployee != null && fullEmployee.getPosition() != null)
        {
            return fullEmployee.getPosition();
        }

        return "Не указана";
    }


    private String buildPrompt(String commentText, double workHours, String position, String taskName)
    {
        return String.format(
                "=== ДАННЫЕ ДЛЯ АНАЛИЗА ===\n" +
                        "Название задачи: \"%s\"\n" +
                        "Должность: %s\n" +
                        "Списано часов: %.1f ч\n" +
                        "Описание работы сотрудника: \"%s\"\n\n" +

                        "=== ТВОЯ ЗАДАЧА ===\n" +
                        "Оцени качество ОПИСАНИЯ РАБОТЫ. Твоя цель - найти ОТКРОВЕННО ПЛОХИЕ комментарии, " +
                        "где невозможно понять, чем человек занимался.\n\n" +

                        "=== ЧТО СЧИТАЕТСЯ НОРМАЛЬНЫМ (PASS) ===\n" +
                        "✓ Есть понимание, чем занимался сотрудник\n" +
                        "✓ Указаны конкретные действия (разработал, исправил, настроил, провел, создал)\n" +
                        "✓ Упоминаются технологии, инструменты или объекты работы\n" +
                        "✓ Для больших часов (5+) - достаточно списка из 3-5 задач или детального описания одной сложной работы\n" +
                        "✓ Технические термины и профессиональный жаргон - это ХОРОШО\n\n" +

                        "=== ЧТО ТОЧНО ПЛОХО (FAIL) ===\n" +
                        "✗ Одно слово: \"работал\", \"занимался\", \"делал\" без уточнений\n" +
                        "✗ Очень короткие описания (меньше 3 слов) для любого времени\n" +
                        "✗ Только повтор названия задачи без деталей\n" +
                        "✗ Для 5+ часов: описание короче 5 слов или полное отсутствие конкретики\n" +
                        "✗ Невозможно понять, что делал человек\n\n" +

                        "=== ПРАВИЛА ПО ВРЕМЕНИ (мягкие) ===\n" +
                        "• 0-1 ч: достаточно одного действия (\"совещание по проекту X\")\n" +
                        "• 1-3 ч: 1-2 конкретных действия\n" +
                        "• 3-5 ч: 2-4 действия или детальное описание\n" +
                        "• 5-8 ч: 3-6 задач или подробное описание сложной работы\n" +
                        "• 8+ ч: список задач или очень детальное описание\n\n" +

                        "=== ОЦЕНКИ ===\n" +
                        "detail_score (0-10): Насколько детально описана работа?\n" +
                        "  0-2: почти нет информации\n" +
                        "  3-5: минимальная информация, но что-то понятно\n" +
                        "  6-7: достаточно деталей\n" +
                        "  8-10: отличное описание\n\n" +

                        "realism_score (0-10): Реально ли описанная работа заняла указанное время?\n" +
                        "  0-3: явно недостаточно работы\n" +
                        "  4-6: возможно, но сомнительно\n" +
                        "  7-10: выглядит реалистично\n\n" +

                        "concrete_score (0-10): Насколько конкретно описание?\n" +
                        "  0-2: только абстрактные фразы\n" +
                        "  3-5: есть некоторая конкретика\n" +
                        "  6-8: много конкретных деталей\n" +
                        "  9-10: максимально конкретное описание\n\n" +

                        "=== ВЕРДИКТ (будь снисходительным!) ===\n" +
                        "FAIL только если:\n" +
                        "  - total_score < 3 (совсем нет информации)\n" +
                        "  - total_score < 4 И часов >= 5\n" +
                        "  - описание короче 3 слов для любого количества часов\n" +
                        "  - только \"работал\" без деталей\n" +
                        "PASS: во всех остальных случаях\n\n" +

                        "=== ПРИМЕРЫ ===\n" +
                        "1ч + \"совещание по проекту X\" → PASS (достаточно)\n" +
                        "3ч + \"работал над задачей\" → FAIL (нет конкретики)\n" +
                        "5ч + \"Разработан модуль с нуля, реализован HTML-шаблон, выполнено тестирование\" → PASS (отлично)\n" +
                        "8ч + \"работал весь день\" → FAIL (нет деталей)\n" +
                        "8ч + \"Завершил разработку, исправил баги, доработал запрос ИИ, собрал контейнер, написал readme, создал инструкции, написал документацию\" → PASS (детально)\n\n" +

                        "ВАЖНО: Будь снисходительным! Цель - отловить только ОТКРОВЕННО плохие комментарии, " +
                        "где вообще непонятно, чем занимался человек. Если есть хоть какая-то конкретика - это PASS.\n\n" +

                        "ВЕРНИ JSON:\n" +
                        "{\n" +
                        "  \"detail_score\": 0-10,\n" +
                        "  \"realism_score\": 0-10,\n" +
                        "  \"concrete_score\": 0-10,\n" +
                        "  \"total_score\": 0-10,\n" +
                        "  \"verdict\": \"PASS\" или \"FAIL\",\n" +
                        "  \"reason\": \"краткое объяснение на русском (максимум 15 слов)\"\n" +
                        "}",
                taskName, position, workHours, commentText
        );
    }
    private String sendAIRequest(String prompt)
    {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "openai");

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Ты проверяешь описания работы. " +
                "Оценивай только КАЧЕСТВО описания: понятно ли, чем занимался человек. " +
                "НЕ важно сколько задач - важна КОНКРЕТИКА. " +
                "1 сложная задача = нормально. " +
                "В reason пиши ТОЛЬКО оценку (макс 10 слов), БЕЗ советов.");
        messages.add(systemMessage);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        messages.add(userMessage);

        requestBody.put("messages", messages);

        String jsonBody = JsonUtil.toJson(requestBody);

        HttpResponse response = HttpBuilder.post(AI_API_URL)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .header("Accept-Charset", "UTF-8")
                .body(jsonBody)
                .executeWithRetry(5);

        if (response.getStatusCode() == 400)
        {
            Logger.warn("AI API заблокировал запрос (content filter). Пропускаем.");
            throw new ContentFilterException("Content filter блокировка");
        }

        if (!response.isSuccess())
        {
            throw new RuntimeException("AI API вернул код " + response.getStatusCode());
        }

        return response.getBody();
    }

    private AIAnalysisResult parseAIResponse(String responseBody)
    {
        try
        {
            Map<String, Object> json = JsonUtil.fromJson(responseBody, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) json.get("choices");

            if (choices == null || choices.isEmpty())
            {
                throw new RuntimeException("Пустой ответ от AI");
            }

            Map<String, Object> firstChoice = choices.get(0);
            Map<String, String> message = (Map<String, String>) firstChoice.get("message");
            String content = message.get("content");

            Map<String, Object> result = JsonUtil.fromJson(content, Map.class);

            if (!result.containsKey("realism_score") ||
                    !result.containsKey("concrete_score") ||
                    !result.containsKey("total_score") ||
                    !result.containsKey("verdict") ||
                    !result.containsKey("reason"))
            {
                Logger.warn("AI вернул неполный JSON: " + content);
                throw new RuntimeException("Неполный ответ от AI - отсутствуют обязательные поля");
            }

            int realismScore = ((Number) result.get("realism_score")).intValue();
            int concreteScore = ((Number) result.get("concrete_score")).intValue();
            double totalScore = ((Number) result.get("total_score")).doubleValue();
            String verdict = (String) result.get("verdict");
            String reason = (String) result.get("reason");

            // detail_score больше нет, передаем 0 или используем concrete_score
            return new AIAnalysisResult(0, realismScore, concreteScore, totalScore, verdict, reason);
        }
        catch (Exception e)
        {
            Logger.error("Ошибка парсинга ответа AI: " + e.getMessage());
            Logger.debug("Ответ AI: " + responseBody);
            throw new RuntimeException("Ошибка парсинга ответа AI", e);
        }
    }

    private String fixEncoding(String text)
    {
        if (text == null || text.isEmpty()) return text;

        long questionMarks = text.chars().filter(ch -> ch == '?').count();
        boolean hasLatin1Artifacts = text.contains("Ð") || text.contains("Ñ") || text.contains("Ð°");

        if (questionMarks > text.length() * 0.2)
        {
            Logger.debug("Обнаружено много '?' в тексте - попытка исправления кодировки");

            try
            {
                byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
                String fixed = new String(bytes, StandardCharsets.UTF_8);
                long fixedQuestions = fixed.chars().filter(ch -> ch == '?').count();

                if (fixedQuestions < questionMarks)
                {
                    Logger.debug("Исправлена кодировка (ISO-8859-1 -> UTF-8)");
                    return fixed;
                }
            } catch (Exception e) {
                Logger.warn("Не удалось исправить через ISO-8859-1: " + e.getMessage());
            }

            try
            {
                byte[] bytes = text.getBytes("windows-1251");
                String fixed = new String(bytes, StandardCharsets.UTF_8);
                long fixedQuestions = fixed.chars().filter(ch -> ch == '?').count();

                if (fixedQuestions < questionMarks)
                {
                    Logger.debug("Исправлена кодировка (Windows-1251 -> UTF-8)");
                    return fixed;
                }
            } catch (Exception e) {
                Logger.warn("Не удалось исправить через Windows-1251: " + e.getMessage());
            }
        }

        if (hasLatin1Artifacts)
        {
            try
            {
                byte[] bytes = text.getBytes(StandardCharsets.ISO_8859_1);
                String fixed = new String(bytes, StandardCharsets.UTF_8);
                Logger.debug("Исправлены Latin-1 артефакты");
                return fixed;
            } catch (Exception e) {
                Logger.warn("Не удалось исправить Latin-1 артефакты: " + e.getMessage());
            }
        }

        return text;
    }
}