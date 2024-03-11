package me.dave.lushrewards.api.event;

import me.dave.lushrewards.data.RewardUser;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

public abstract class RewardUserEvent extends Event {
    protected final RewardUser rewardUser;

    public RewardUserEvent(@NotNull final RewardUser rewardUser) {
        this.rewardUser = rewardUser;
    }

    public RewardUserEvent(@NotNull final RewardUser rewardUser, boolean async) {
        super(async);
        this.rewardUser = rewardUser;
    }

    /**
     * Returns the player involved in this event
     *
     * @return Player who is involved in this event
     */
    @NotNull
    public final RewardUser getRewardUser() {
        return rewardUser;
    }
}