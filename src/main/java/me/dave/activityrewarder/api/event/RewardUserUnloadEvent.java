package me.dave.activityrewarder.api.event;

import me.dave.activityrewarder.data.RewardUser;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RewardUserUnloadEvent extends RewardUserEvent implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private boolean cancel = false;

    public RewardUserUnloadEvent(@NotNull RewardUser rewardUser) {
        super(rewardUser);
    }

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
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
