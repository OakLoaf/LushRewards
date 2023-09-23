package me.dave.activityrewarder.api.event;

import me.dave.activityrewarder.data.RewardUser;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RewardUserPlaytimeChangeEvent extends RewardUserEvent {
    private static final HandlerList handlers = new HandlerList();
    private final int oldMinutesPlayed;
    private final int newMinutesPlayed;

    public RewardUserPlaytimeChangeEvent(@NotNull RewardUser rewardUser, int oldMinutesPlayed, int newMinutesPlayed) {
        super(rewardUser);
        this.oldMinutesPlayed = oldMinutesPlayed;
        this.newMinutesPlayed = newMinutesPlayed;
    }

    public int getOldMinutesPlayed() {
        return oldMinutesPlayed;
    }

    public int getNewMinutesPlayed() {
        return newMinutesPlayed;
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
