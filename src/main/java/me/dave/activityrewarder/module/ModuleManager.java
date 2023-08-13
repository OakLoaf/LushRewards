package me.dave.activityrewarder.module;

import java.util.HashMap;

public class ModuleManager {
    private final HashMap<String, Module> modules = new HashMap<>();

    public Module getModule(String id) {
        return modules.get(id);
    }

    public void registerModule(Module module) {
        modules.put(module.getId(), module);
        module.enable();
    }
}
