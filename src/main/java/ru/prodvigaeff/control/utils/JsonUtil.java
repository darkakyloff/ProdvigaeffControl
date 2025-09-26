package ru.prodvigaeff.control.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonUtil
{
    private static final ObjectMapper mapper = new ObjectMapper();

    static
    {
        mapper.registerModule(new JavaTimeModule());
    }

    public static String toJson(Object obj)
    {
        if (obj == null)
        {
            Logger.warn("Попытка сериализации null объекта");
            return null;
        }

        try
        {
            return mapper.writeValueAsString(obj);
        }
        catch (Exception e)
        {
            Logger.error("Ошибка сериализации в JSON для объекта " + obj.getClass().getSimpleName() + ": " + e.getMessage());
            throw new JsonProcessingException("Не удалось сериализовать объект", e);
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz)
    {
        if (StringUtil.isEmpty(json))
        {
            Logger.warn("Попытка десериализации пустого JSON");
            return null;
        }

        if (clazz == null)
        {
            throw new IllegalArgumentException("Класс для десериализации не может быть null");
        }

        try
        {
            return mapper.readValue(json, clazz);
        }
        catch (Exception e)
        {
            Logger.error("Ошибка десериализации из JSON в " + clazz.getSimpleName() + ": " + e.getMessage());
            Logger.debug("JSON содержимое", json.length() > 500 ? json.substring(0, 500) + "..." : json);
            throw new JsonProcessingException("Не удалось десериализовать JSON в " + clazz.getSimpleName(), e);
        }
    }

    public static boolean isValidJson(String json)
    {
        if (StringUtil.isEmpty(json)) return false;

        try
        {
            mapper.readTree(json);
            return true;
        }
        catch (Exception e)
        {
            Logger.debug("JSON валидация", "Невалидный JSON: " + e.getMessage());
            return false;
        }
    }

    public static class JsonProcessingException extends RuntimeException
    {
        public JsonProcessingException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}