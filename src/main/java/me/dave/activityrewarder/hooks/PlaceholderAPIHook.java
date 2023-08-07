package me.dave.activityrewarder.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.utils.LocalPlaceholders;
import org.bukkit.entity.Player;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();

    public String onPlaceholderRequest(Player player, String params) {
        return LocalPlaceholders.parsePlaceholder(player, params);
    }

    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return true;
    }

    public String getIdentifier() {
        return "rewarder";
    }

    public String getAuthor() {
        return this.plugin.getDescription().getAuthors().toString();
    }

    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }
}
