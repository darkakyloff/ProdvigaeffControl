package ru.prodvigaeff.control.core.exception;

import ru.prodvigaeff.control.utils.Logger;

public class ErrorHandler
{
    public static void handle(String message, Exception e)
    {
        Logger.error(message + ": " + e.getMessage());
        
        String logLevel = System.getProperty("workguard.env", "prod");
        if ("dev".equals(logLevel))
        {
            e.printStackTrace();
        }
    }
    
    public static void handle(Exception e)
    {
        handle("Необработанная ошибка", e);
    }
}