package ru.prodvigaeff.control.core.module;

import ru.prodvigaeff.control.utils.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleRegistry
{
    private static final Map<String, AbstractModule> modules = new HashMap<>();
    
    public static void register(AbstractModule module)
    {
        String name = module.getName();
        if (modules.containsKey(name))
        {
            Logger.warn("Модуль " + name + " уже зарегистрирован");
            return;
        }
        
        modules.put(name, module);
        Logger.info("Модуль зарегистрирован: " + name);
    }
    
    public static AbstractModule getModule(String name)
    {
        return modules.get(name);
    }
    
    public static List<AbstractModule> getAllModules()
    {
        return new ArrayList<>(modules.values());
    }
    
    public static void executeModule(String name)
    {
        AbstractModule module = modules.get(name);
        if (module != null)
        {
            module.execute();
        }
        else
        {
            Logger.error("Модуль не найден: " + name);
        }
    }
    
    public static int getModuleCount()
    {
        return modules.size();
    }
}