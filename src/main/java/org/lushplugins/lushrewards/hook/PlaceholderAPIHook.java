package org.lushplugins.lushrewards.hook;

import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushlib.hook.Hook;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIHook extends Hook {
    private PlaceholderExpansion expansion;
    private OutdatedPlaceholderExpansion outdatedExpansion;

    public PlaceholderAPIHook() {
        super(HookId.PLACEHOLDER_API.toString());
    }

    @Override
    public void onEnable() {
        expansion = new PlaceholderExpansion();
        expansion.register();

        outdatedExpansion = new OutdatedPlaceholderExpansion();
        outdatedExpansion.register();
    }

    @Override
    protected void onDisable() {
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
        }

        if (outdatedExpansion != null) {
            outdatedExpansion.unregister();
            outdatedExpansion = null;
        }
    }

    public static class PlaceholderExpansion extends me.clip.placeholderapi.expansion.PlaceholderExpansion {

        public String onPlaceholderRequest(Player player, @NotNull String params) {
            return LushRewards.getInstance().getLocalPlaceholders().parsePlaceholder(params, player);
        }

        public boolean persist() {
            return true;
        }

        public boolean canRegister() {
            return true;
        }

        @NotNull
        public String getIdentifier() {
            return "lushrewards";
        }

        @NotNull
        public String getAuthor() {
            return LushRewards.getInstance().getDescription().getAuthors().toString();
        }

        @NotNull
        public String getVersion() {
            return LushRewards.getInstance().getDescription().getVersion();
        }
    }

    @Deprecated(since = "3.0.0")
    public static class OutdatedPlaceholderExpansion extends me.clip.placeholderapi.expansion.PlaceholderExpansion {

        public String onPlaceholderRequest(Player player, @NotNull String params) {
            return LushRewards.getInstance().getLocalPlaceholders().parsePlaceholder(params, player);
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
            return LushRewards.getInstance().getDescription().getAuthors().toString();
        }

        @NotNull
        public String getVersion() {
            return LushRewards.getInstance().getDescription().getVersion();
        }
    }
}
