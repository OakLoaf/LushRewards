package me.dave.activityrewarder.hooks;

public enum HookId {
    PLACEHOLDER_API("placeholder-api"),
    FLOODGATE("floodgate");


    private final String id;

    HookId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
