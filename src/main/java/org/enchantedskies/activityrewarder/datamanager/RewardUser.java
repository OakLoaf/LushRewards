package org.enchantedskies.activityrewarder.datamanager;

import org.enchantedskies.activityrewarder.ActivityRewarder;

import java.time.LocalDate;
import java.util.UUID;

public class RewardUser {
    private final UUID uuid;
    private String username;
    private final LocalDate startDate;
    private LocalDate latestDate;
    private int dayNum;
    private int playTime;

    public RewardUser(UUID uuid, String username, String startDate, String lastCollectedDate, int dayNum, int playTime) {
        this.uuid = uuid;
        this.username = username;
        this.startDate = LocalDate.parse(startDate);
        this.latestDate = LocalDate.parse(lastCollectedDate);
        this.dayNum = dayNum;
        this.playTime = playTime;
    }

    public void setUsername(String username) {
        this.username = username;
        ActivityRewarder.dataManager.saveRewardUser(this);
    }

    public void setLatestDate(String lastCollectedDate) {
        this.latestDate = LocalDate.parse(lastCollectedDate);
        ActivityRewarder.dataManager.saveRewardUser(this);
    }

    public void incrementDayNum() {
        this.dayNum += 1;
        ActivityRewarder.dataManager.saveRewardUser(this);
    }

    public void setPlayTime(int playTime) {
        this.playTime = playTime;
        ActivityRewarder.dataManager.saveRewardUser(this);
    }

    public UUID getUUID() {
        return this.uuid;
    }

    public String getUsername() {
        return this.username;
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public LocalDate getLatestDate() {
        return this.latestDate;
    }

    public int getDayNum() {
        return this.dayNum;
    }

    public int getPlayTime() {
        return this.playTime;
    }
}
