package me.dave.activityrewarder.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.utils.LocalPlaceholders;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIHook extends PlaceholderExpansion {
    private final ActivityRewarder plugin = ActivityRewarder.getInstance();

    public String onPlaceholderRequest(Player player, @NotNull String params) {
        return LocalPlaceholders.parsePlaceholder(params, player);
    }

    public boolean persist() {
        return true;
    }

    public boolean canRegister() {
        return true;
    }

    @NotNull
    public String getIdentifier() {
        return "rewarder";
    }

    @NotNull
    public String getAuthor() {
        return this.plugin.getDescription().getAuthors().toString();
    }

    @NotNull
    public String getVersion() {
        return this.plugin.getDescription().getVersion();
    }
}
