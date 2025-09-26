package ru.prodvigaeff.control.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil
{
    public static final ZoneId MSK_ZONE = ZoneId.of("Europe/Moscow");
    public static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    
    public static LocalDateTime parseIsoToMsk(String isoString)
    {
        try
        {
            String cleanIso = isoString.replace("Z", "+00:00");
            ZonedDateTime utc = ZonedDateTime.parse(cleanIso);
            return utc.withZoneSameInstant(MSK_ZONE).toLocalDateTime();
        }
        catch (Exception e)
        {
            Logger.error("Ошибка парсинга даты: " + isoString);
            return null;
        }
    }
    
    public static LocalDate today()
    {
        return LocalDate.now(MSK_ZONE);
    }
    
    public static LocalDateTime todayStart()
    {
        return today().atStartOfDay();
    }
    
    public static LocalDateTime todayEnd()
    {
        return today().atTime(23, 59, 59);
    }
    
    public static boolean isToday(LocalDateTime dateTime)
    {
        return dateTime.toLocalDate().equals(today());
    }
    
    public static boolean isSameDate(LocalDateTime date1, LocalDateTime date2)
    {
        return date1.toLocalDate().equals(date2.toLocalDate());
    }
    
    public static int daysDifference(LocalDate date1, LocalDate date2)
    {
        return (int) (date1.toEpochDay() - date2.toEpochDay());
    }
}