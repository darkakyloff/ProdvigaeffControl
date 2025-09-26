package ru.prodvigaeff.control.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger
{
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String GRAY = "\u001B[90m";
    private static final String PURPLE = "\u001B[35m";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static String lastProgressLine = "";

    public static void debug(String message)
    {
        String logLevel = EnvUtil.get("APP_LOG_LEVEL", "INFO");
        if (!"DEBUG".equals(logLevel)) return;
        print(PURPLE, "ОТЛАДКА", message);
    }

    public static void debug(String context, String message)
    {
        String logLevel = EnvUtil.get("APP_LOG_LEVEL", "INFO");
        if (!"DEBUG".equals(logLevel)) return;
        print(PURPLE, "ОТЛАДКА[" + context + "]", message);
    }

    public static void debugRequest(String method, String url, String body)
    {
        debug("HTTP", method + " " + url);
        if (body != null && !body.isEmpty()) debug("BODY", body);
    }

    public static void debugResponse(int code, String response)
    {
        debug("RESPONSE", "HTTP " + code + " длина: " + (response != null ? response.length() : 0));
        if (response != null && response.length() < 500) debug("RESPONSE_BODY", response);
    }

    public static void info(String message)
    {
        clearProgressLine();
        print(BLUE, "ИНФО", message);
    }

    public static void success(String message)
    {
        clearProgressLine();
        print(GREEN, "ОК", message);
    }

    public static void warn(String message)
    {
        clearProgressLine();
        print(YELLOW, "ВНИМАНИЕ", message);
    }

    public static void error(String message)
    {
        clearProgressLine();
        print(RED, "ОШИБКА", message);
    }

    public static void section(String title)
    {
        clearProgressLine();
        System.out.println();
        print(CYAN, "РАЗДЕЛ", "=== " + title.toUpperCase() + " ===");
    }

    public static void result(String operation, int count, long timeMs)
    {
        clearProgressLine();
        String time = formatTime(timeMs);
        print(GREEN, "РЕЗУЛЬТАТ", operation + " завершено | Найдено: " + count + " | Время: " + time);
    }

    public static void progress(String operation, int current, int total)
    {
        if (current % 10 == 0 || current == total)
        {
            String progressBar = createProgressBar(current, total, 30);
            String line = String.format("\r%s[%s] %s%s %s (%d/%d)%s",
                    GRAY, LocalDateTime.now().format(TIME_FORMAT), CYAN, operation,
                    progressBar, current, total, RESET);

            System.out.print(line);
            lastProgressLine = line;

            if (current == total)
            {
                System.out.println();
                lastProgressLine = "";
            }
        }
    }

    private static void clearProgressLine()
    {
        if (!lastProgressLine.isEmpty())
        {
            System.out.print("\r" + " ".repeat(lastProgressLine.length()) + "\r");
            lastProgressLine = "";
        }
    }

    private static String createProgressBar(int current, int total, int width)
    {
        if (total == 0) return "[" + " ".repeat(width) + "]";

        int filled = (int) ((double) current / total * width);
        return "[" + "=".repeat(filled) + " ".repeat(width - filled) + "]";
    }

    private static void print(String color, String level, String message)
    {
        String timestamp = LocalDateTime.now().format(TIME_FORMAT);
        try
        {
            System.out.println(color + "[" + timestamp + "] " + level + " " + RESET + message);
        }
        catch (Exception e)
        {
            System.out.println("[" + timestamp + "] " + level + " " + message);
        }
    }

    private static String formatTime(long milliseconds)
    {
        return milliseconds < 1000 ? milliseconds + "мс" :
                milliseconds < 60000 ? String.format("%.1fс", milliseconds / 1000.0) :
                        String.format("%d:%02d", milliseconds / 60000, (milliseconds % 60000) / 1000);
    }
}