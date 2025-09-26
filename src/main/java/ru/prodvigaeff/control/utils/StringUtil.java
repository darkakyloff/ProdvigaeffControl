package ru.prodvigaeff.control.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class StringUtil
{
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*$"
    );

    public static String urlEncode(String value)
    {
        if (value == null) return "";

        try
        {
            return URLEncoder.encode(value, StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            Logger.warn("Ошибка URL-кодирования строки: " + value);
            return value;
        }
    }

    public static String cleanHtml(String content)
    {
        if (content == null) return "не найдено";

        return content
                .replaceAll("<[^>]*>", "") // Удаляем HTML теги
                .replaceAll("&nbsp;", " ") // Заменяем неразрывные пробелы
                .replaceAll("&amp;", "&")  // Декодируем HTML entities
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("&quot;", "\"")
                .replaceAll("&#39;", "'")
                .replaceAll("\\s+", " ")   // Схлопываем множественные пробелы
                .trim();
    }

    public static boolean isEmpty(String str)
    {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isNotEmpty(String str)
    {
        return !isEmpty(str);
    }

    public static boolean isValidEmail(String email)
    {
        if (isEmpty(email)) return false;

        if (email.length() > 254) return false;

        if (email.contains("..") || email.startsWith(".") || email.endsWith(".")) return false;

        return EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isValidCompanyEmail(String email, String companyDomain)
    {
        if (!isValidEmail(email)) return false;

        if (isEmpty(companyDomain)) return true;

        return email.toLowerCase().endsWith("@" + companyDomain.toLowerCase());
    }

    public static String truncate(String str, int maxLength)
    {
        if (isEmpty(str) || maxLength < 0) return str;

        if (str.length() <= maxLength) return str;

        return str.substring(0, maxLength - 3) + "...";
    }

    public static String sanitizeForLog(String str)
    {
        if (isEmpty(str)) return str;

        return str.replaceAll("[\r\n\t]", "_")
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
    }

    public static String maskEmail(String email)
    {
        if (!isValidEmail(email)) return email;

        String[] parts = email.split("@");
        if (parts.length != 2) return email;

        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) return email;

        String masked = username.charAt(0) + "*".repeat(username.length() - 2) + username.charAt(username.length() - 1);
        return masked + "@" + domain;
    }
}