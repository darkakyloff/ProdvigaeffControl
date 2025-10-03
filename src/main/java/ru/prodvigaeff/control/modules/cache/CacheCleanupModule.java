package ru.prodvigaeff.control.modules.cache;

import ru.prodvigaeff.control.core.module.AbstractModule;
import ru.prodvigaeff.control.megaplan.managers.MegaplanTask;
import ru.prodvigaeff.control.utils.Logger;

public class CacheCleanupModule extends AbstractModule
{
    @Override
    public String getName()
    {
        return "CacheCleanup";
    }

    @Override
    public String getCronExpression()
    {
        return "0 0 * * * *";
    }

    @Override
    public void executeModule()
    {
        Logger.debug("Начинаем очистку устаревших записей в кеше");
        
        long startTime = System.currentTimeMillis();
        
        MegaplanTask.clearEmployeeCache();
        
        long endTime = System.currentTimeMillis();
        Logger.result("Очистка кеша", 1, endTime - startTime);
    }
}