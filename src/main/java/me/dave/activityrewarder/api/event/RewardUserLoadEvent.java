package me.dave.activityrewarder.api.event;

import me.dave.activityrewarder.data.RewardUser;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RewardUserLoadEvent extends RewardUserEvent {
    private static final HandlerList handlers = new HandlerList();

    public RewardUserLoadEvent(@NotNull RewardUser rewardUser) {
        super(rewardUser);
    }

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    @NotNull
    public static HandlerList getHandlerList() {
        return handlers;
    }
}
