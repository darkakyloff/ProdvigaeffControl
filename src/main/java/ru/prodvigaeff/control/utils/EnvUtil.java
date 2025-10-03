package ru.prodvigaeff.control.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class EnvUtil
{
    private static final Map<String, String> env = new HashMap<>();

    public static void load()
    {
        try (BufferedReader reader = new BufferedReader(new FileReader(".env")))
        {
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.trim().isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split("=", 2);
                if (parts.length == 2)
                {
                    env.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
        catch (IOException e)
        {
            Logger.warn("Не удалось загрузить .env файл: " + e.getMessage());
        }
    }

    public static String get(String key)
    {
        String value = env.get(key);
        return value != null ? value : System.getenv(key);
    }

    public static String get(String key, String defaultValue)
    {
        String value = get(key);
        return value != null ? value : defaultValue;
    }

    public static int getInt(String key, int defaultValue)
    {
        String value = get(key);
        if (value == null) return defaultValue;

        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue)
    {
        String value = get(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    public static List<String> getList(String key)
    {
        String value = get(key);
        if (value == null || value.trim().isEmpty()) return new ArrayList<>();

        value = value.trim();
        if (value.startsWith("'") && value.endsWith("'"))
        {
            value = value.substring(1, value.length() - 1);
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public static List<String> getList(String key, List<String> defaultValue)
    {
        List<String> result = getList(key);
        return result.isEmpty() ? defaultValue : result;
    }
}