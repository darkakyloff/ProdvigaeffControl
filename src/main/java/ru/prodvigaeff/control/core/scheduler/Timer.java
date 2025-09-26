package ru.prodvigaeff.control.core.scheduler;

import ru.prodvigaeff.control.core.module.AbstractModule;
import ru.prodvigaeff.control.core.module.ModuleRegistry;
import ru.prodvigaeff.control.utils.Logger;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Timer
{
    private static ScheduledExecutorService scheduler;
    private static final Map<String, CronExpression> cronExpressions = new HashMap<>();

    public static void start()
    {
        if (scheduler != null) return;

        scheduler = Executors.newScheduledThreadPool(2);

        for (AbstractModule module : ModuleRegistry.getAllModules())
        {
            if (!module.isEnabled()) continue;

            String cronExpr = module.getCronExpression();
            if (cronExpr != null && !cronExpr.isEmpty())
            {
                cronExpressions.put(module.getName(), new CronExpression(cronExpr));
                Logger.info("Модуль " + module.getName() + " запланирован: " + cronExpr);
            }
            else
            {
                Logger.info("Модуль " + module.getName() + " выполняется один раз при старте");
                scheduler.execute(module::execute);
            }
        }

        scheduler.scheduleAtFixedRate(Timer::checkSchedules, 0, 1, TimeUnit.SECONDS);
        Logger.info("Планировщик запущен");
    }

    public static void stop()
    {
        if (scheduler != null)
        {
            scheduler.shutdown();
            try
            {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS))
                {
                    scheduler.shutdownNow();
                }
            }
            catch (InterruptedException e)
            {
                scheduler.shutdownNow();
            }
            Logger.info("Планировщик остановлен");
        }
    }

    private static void checkSchedules()
    {
        LocalDateTime now = LocalDateTime.now();

        for (AbstractModule module : ModuleRegistry.getAllModules())
        {
            if (!module.isEnabled()) continue;

            CronExpression cronExpr = cronExpressions.get(module.getName());
            if (cronExpr != null && cronExpr.matches(now))
            {
                scheduler.execute(module::execute);
            }
        }
    }
}