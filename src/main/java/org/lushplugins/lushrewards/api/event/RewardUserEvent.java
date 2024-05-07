package org.lushplugins.lushrewards.api.event;

import org.lushplugins.lushrewards.data.RewardUser;
import org.bukkit.event.Event;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
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