package me.dave.lushrewards.data;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.api.event.RewardUserPlaytimeChangeEvent;
import me.dave.lushrewards.module.UserDataModule;
import me.dave.platyutils.PlatyUtils;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RewardUser extends UserDataModule.UserData {
    private String username;
    private int minutesPlayed;

    public RewardUser(@NotNull UUID uuid, String username, int minutesPlayed) {
        super(uuid, null);
        this.username = username;
        this.minutesPlayed = minutesPlayed;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
        LushRewards.getInstance().getDataManager().saveRewardUser(this);
    }

    public int getMinutesPlayed() {
        return this.minutesPlayed;
    }

    public void setMinutesPlayed(int minutesPlayed) {
        PlatyUtils.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> LushRewards.getInstance().callEvent(new RewardUserPlaytimeChangeEvent(this, this.minutesPlayed, minutesPlayed)));

        this.minutesPlayed = minutesPlayed;
        LushRewards.getInstance().getDataManager().saveRewardUser(this);
    }

    public void save() {
        LushRewards.getInstance().getDataManager().saveRewardUser(this);
    }
}
