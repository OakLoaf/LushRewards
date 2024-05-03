package me.dave.lushrewards.hook;

import me.dave.lushrewards.LushRewards;
import org.lushplugins.lushlib.hook.Hook;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PlaceholderAPIHook extends Hook {
    private PlaceholderExpansion expansion;

    public PlaceholderAPIHook() {
        super(HookId.PLACEHOLDER_API.toString());
    }

    @Override
    public void onEnable() {
        expansion = new PlaceholderExpansion();
        expansion.register();
    }

    @Override
    protected void onDisable() {
        if (expansion != null) {
            expansion.unregister();
            expansion = null;
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
