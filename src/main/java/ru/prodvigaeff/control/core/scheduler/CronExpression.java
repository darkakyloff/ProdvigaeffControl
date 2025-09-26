package ru.prodvigaeff.control.core.scheduler;

import java.time.LocalDateTime;
import java.util.Arrays;

public class CronExpression
{
    private final String expression;
    private final int[] seconds;
    private final int[] minutes;
    private final int[] hours;
    private final int[] days;
    private final int[] months;
    private final int[] weekdays;
    
    public CronExpression(String expression)
    {
        this.expression = expression;
        String[] parts = expression.split(" ");
        
        if (parts.length != 6) throw new IllegalArgumentException("Неверный формат cron: " + expression);
        
        this.seconds = parseField(parts[0], 0, 59);
        this.minutes = parseField(parts[1], 0, 59);
        this.hours = parseField(parts[2], 0, 23);
        this.days = parseField(parts[3], 1, 31);
        this.months = parseField(parts[4], 1, 12);
        this.weekdays = parseField(parts[5], 0, 6);
    }
    
    public boolean matches(LocalDateTime dateTime)
    {
        return matches(seconds, dateTime.getSecond()) &&
               matches(minutes, dateTime.getMinute()) &&
               matches(hours, dateTime.getHour()) &&
               matches(days, dateTime.getDayOfMonth()) &&
               matches(months, dateTime.getMonthValue()) &&
               matches(weekdays, dateTime.getDayOfWeek().getValue() % 7);
    }
    
    private boolean matches(int[] values, int value)
    {
        return Arrays.stream(values).anyMatch(v -> v == value);
    }
    
    private int[] parseField(String field, int min, int max)
    {
        if ("*".equals(field))
        {
            return range(min, max);
        }
        
        if (field.contains(","))
        {
            return Arrays.stream(field.split(","))
                         .mapToInt(Integer::parseInt)
                         .toArray();
        }
        
        return new int[]{Integer.parseInt(field)};
    }
    
    private int[] range(int min, int max)
    {
        int[] result = new int[max - min + 1];
        for (int i = 0; i < result.length; i++)
        {
            result[i] = min + i;
        }
        return result;
    }
    
    @Override
    public String toString()
    {
        return expression;
    }
}