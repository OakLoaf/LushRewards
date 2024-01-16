package me.dave.activityrewarder.hooks;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.utils.LocalPlaceholders;
import me.dave.platyutils.hook.Hook;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIHook extends Hook {
    private ActivityRewarderExpansion expansion;

    public PlaceholderAPIHook() {
        super(HookId.PLACEHOLDER_API.toString());
    }

    @Override
    public void onEnable() {
        expansion = new ActivityRewarderExpansion();
        expansion.register();
    }

    @Override
    protected void onDisable() {
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }
    }

    public static class ActivityRewarderExpansion extends PlaceholderExpansion {

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
            return ActivityRewarder.getInstance().getDescription().getAuthors().toString();
        }

        @NotNull
        public String getVersion() {
            return ActivityRewarder.getInstance().getDescription().getVersion();
        }
    }
}
