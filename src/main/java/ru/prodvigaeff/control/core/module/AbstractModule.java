package ru.prodvigaeff.control.core.module;

import ru.prodvigaeff.control.core.exception.ErrorHandler;
import ru.prodvigaeff.control.utils.Logger;

public abstract class AbstractModule
{
    public abstract String getName();
    public abstract String getCronExpression();
    public abstract void executeModule();
    
    public final void execute()
    {
        Logger.info("Запуск модуля: " + getName());
        
        try
        {
            executeModule();
            Logger.info("Модуль завершен: " + getName());
        }
        catch (Exception e)
        {
            ErrorHandler.handle("Ошибка в модуле " + getName(), e);
        }
    }
    
    public boolean isEnabled()
    {
        return true;
    }
}