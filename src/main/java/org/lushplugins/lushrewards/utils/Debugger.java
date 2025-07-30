package org.lushplugins.lushrewards.utils;

import org.lushplugins.lushrewards.LushRewards;

import java.util.logging.Logger;

public class Debugger {
    private static DebugMode debugMode = DebugMode.NONE;

    public static void sendDebugMessage(String string, DebugMode mode) {
        if (debugMode == mode || debugMode == DebugMode.ALL) {
            LushRewards.getInstance().getLogger().info("DEBUG >> " + string);
        }
    }

    public static void setDebugMode(DebugMode debugMode) {
        Debugger.debugMode = debugMode;
    }

    // TODO: Move debug toggle into individual reward modules (in addition to overall debug mode)
    public enum DebugMode {
        NONE,
        PLAYTIME,
        DAILY,
        ALL
    }
}
