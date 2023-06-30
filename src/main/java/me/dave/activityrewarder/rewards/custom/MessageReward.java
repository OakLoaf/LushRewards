package me.dave.activityrewarder.rewards.custom;

import me.dave.activityrewarder.rewards.Reward;
import me.dave.chatcolorhandler.ChatColorHandler;
import org.bukkit.entity.Player;

public class MessageReward implements Reward {
    private final String message;

    public MessageReward(String message) {
        this.message = message;
    }

    @Override
    public void giveReward(Player player) {
        ChatColorHandler.sendMessage(player, message);
    }
}
