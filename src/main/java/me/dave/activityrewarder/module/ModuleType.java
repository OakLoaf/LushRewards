package me.dave.activityrewarder.module;

public enum ModuleType {
    DAILY_REWARDS("daily_rewards"),
    ONE_TIME_REWARDS("one_time_rewards"),
    PLAYTIME_REWARDS("playtime_rewards"),
    PLAYTIME_TRACKER("playtime_tracker");

    private final String id;

    ModuleType(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }
}
