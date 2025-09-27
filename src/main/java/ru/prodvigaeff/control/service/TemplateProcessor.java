package ru.prodvigaeff.control.service;

import ru.prodvigaeff.control.utils.StringUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class TemplateProcessor
{
    public String processTemplate(String templateName, Map<String, String> data)
    {
        String template = loadTemplate(templateName);
        return replacePlaceholders(template, data);
    }

    private String loadTemplate(String templateName)
    {
        String resourcePath = "templates/" + templateName;
        
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath))
        {
            if (inputStream == null) throw new RuntimeException("Шаблон не найден: " + resourcePath);
            return new String(inputStream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new RuntimeException("Ошибка загрузки шаблона: " + templateName, e);
        }
    }

    private String replacePlaceholders(String content, Map<String, String> placeholders)
    {
        if (placeholders == null || placeholders.isEmpty()) return content;

        String result = content;
        for (Map.Entry<String, String> entry : placeholders.entrySet())
        {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";

            if ("VIOLATIONS_LIST".equals(key)) result = result.replace("{" + key + "}", value);
            else result = result.replace("{" + key + "}", escapeHtml(value));

        }
        return result;
    }

    private String escapeHtml(String text)
    {
        if (StringUtil.isEmpty(text)) return text;
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}
