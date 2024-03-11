package me.dave.lushrewards.utils;

import me.dave.lushrewards.LushRewards;

import java.util.logging.Logger;

public class Debugger {
    private static Logger logger;
    private static DebugMode debugMode = DebugMode.NONE;

    public static void sendDebugMessage(String string, DebugMode mode) {
        if (debugMode == mode || debugMode == DebugMode.ALL) {
            getOrInitLogger().info("DEBUG >> " + string);
        }
    }

    public static void setDebugMode(DebugMode debugMode) {
        Debugger.debugMode = debugMode;
    }

    private static Logger getOrInitLogger() {
        if (logger == null) {
            logger = LushRewards.getInstance().getLogger();
        }

        return logger;
    }

    public enum DebugMode {
        NONE,
        PLAYTIME,
        DAILY,
        ALL
    }
}
