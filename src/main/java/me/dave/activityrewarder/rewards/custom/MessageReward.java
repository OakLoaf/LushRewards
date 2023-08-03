package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.rewards.Reward;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class MessageReward implements Reward {
    private final String message;

    public MessageReward(String message) {
        this.message = message;
    }

    public MessageReward(ConfigurationSection configurationSection) {
        this.message = configurationSection.getString("message");
    }

    @Override
    public void giveTo(Player player) {
        ChatColorHandler.sendMessage(player, message);
    }
}
