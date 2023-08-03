package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.rewards.Reward;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class BroadcastReward implements Reward {
    private final String message;

    public BroadcastReward(String message) {
        this.message = message;
    }

    public BroadcastReward(ConfigurationSection configurationSection) {
        this.message = configurationSection.getString("message");
    }

    @Override
    public void giveTo(Player player) {
        ChatColorHandler.broadcastMessage(message);
    }
}
