package org.lushplugins.lushrewards.utils;

import org.lushplugins.guihandler.gui.GuiLayer;
import org.lushplugins.lushrewards.LushRewards;

public class GuiTemplates {

    public static final GuiLayer DEFAULT = new GuiLayer(
        "RRRRRRRRR",
        "RRRRRRRRR",
        "RRRRRRRRR",
        "RRRR#####",
        "#########",
        "####P####"
    );

    public static final GuiLayer COMPACT = new GuiLayer(
        "RRRRRRR N"
    );

    public static final GuiLayer COMPACT_PROFILE = new GuiLayer(
        "RRRRRRR P"
    );

    public static final GuiLayer BORDERED = new GuiLayer(
        "#########",
        "RRRRRRR#N",
        "#########"
    );

    public static final GuiLayer BORDERED_LARGE = new GuiLayer(
        "#########",
        "#RRRRRRR#",
        "#RRRRRRR#",
        "#RRRRRRR#",
        "#RRRRRRR#",
        "####P####"
    );

    public static final GuiLayer DAILY_REWARDS_PLUS = new GuiLayer(
        "RRRRRRRRR",
        "RRRRRRRRR",
        "#RRRRRRR#",
        "##RRRRR##",
        "#########",
        "####P####"
    );

    public static final GuiLayer NDAILY_REWARDS = new GuiLayer("#########",
        "#RRRRRRR#",
        "#########"
    );

    public static GuiLayer valueOf(String string) {
        return switch (string.toUpperCase()) {
            case "DEFAULT" -> DEFAULT;
            case "COMPACT" -> COMPACT;
            case "COMPACT_PROFILE" -> COMPACT_PROFILE;
            case "BORDERED" -> BORDERED;
            case "BORDERED_LARGE" -> BORDERED_LARGE;
            case "DAILY_REWARDS_PLUS" -> DAILY_REWARDS_PLUS;
            case "NDAILY_REWARDS" -> NDAILY_REWARDS;
            default -> {
                LushRewards.getInstance().getLogger().warning("Invalid template type, setting to default.");
                yield DEFAULT;
            }
        };
    }
}
