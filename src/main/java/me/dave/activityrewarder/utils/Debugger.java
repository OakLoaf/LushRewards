package me.dave.activityrewarder.utils;

import me.dave.activityrewarder.ActivityRewarder;

import java.util.logging.Logger;

public class Debugger {
    private static Logger logger;

    public static void sendDebugMessage(String string, DebugMode mode) {
        if (ActivityRewarder.configManager.getDebugMode() == mode || ActivityRewarder.configManager.getDebugMode() == DebugMode.ALL) getOrInitLogger().info("DEBUG >> " + string);
    }

    private static Logger getOrInitLogger() {
        if (logger == null) logger = ActivityRewarder.getInstance().getLogger();
        return logger;
    }

    public enum DebugMode {
        NONE,
        HOURLY,
        DAILY,
        ALL
    }
}
