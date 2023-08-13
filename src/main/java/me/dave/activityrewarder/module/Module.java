package me.dave.activityrewarder.module;

import org.jetbrains.annotations.NotNull;

public abstract class Module {
    private final String id;
    private boolean enabled = false;

    public Module(String id) {
        this.id = id.toLowerCase();
    }

    public void enable() {
        this.enabled = true;
        this.onEnable();
    }

    public void onEnable() {}

    public void disable () {
        this.enabled = false;
        this.onDisable();
    }
    public void onDisable() {}

    @NotNull
    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
