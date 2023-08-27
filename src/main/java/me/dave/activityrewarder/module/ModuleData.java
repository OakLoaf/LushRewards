package me.dave.activityrewarder.module;

public abstract class ModuleData {
    private final String id;

    public ModuleData(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
