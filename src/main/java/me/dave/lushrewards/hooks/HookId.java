package me.dave.lushrewards.hooks;

public enum HookId {
    FLOODGATE("floodgate"),
    PLACEHOLDER_API("placeholder-api");

    private final String id;

    HookId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}