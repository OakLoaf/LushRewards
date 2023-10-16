package me.dave.activityrewarder.importer;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModule;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModuleUserData;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ActivityRewarderDataUpdater extends ConfigImporter {

    public ActivityRewarderDataUpdater() throws FileNotFoundException {
        super();
    }

    @Override
    protected String getPluginName() {
        return "ActivityRewarder";
    }

    @Override
    public CompletableFuture<Boolean> startImport() {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

        ActivityRewarder.getMorePaperLib().scheduling().asyncScheduler().run(() -> {
            File playerDataFile = new File(dataFolder, "data");
            File oldPlayerDataFile = new File(dataFolder, "data-old");
            if (!playerDataFile.exists()) {
                return;
            }

            if (!playerDataFile.renameTo(oldPlayerDataFile)) {
                ActivityRewarder.getInstance().getLogger().severe("Failed to rename 'data' directory");
                return;
            }

            int successes = 0;
            int fails = 0;
            File[] dataFiles = oldPlayerDataFile.listFiles();
            if (dataFiles != null) {
                for (File dataFile : dataFiles) {
                    YamlConfiguration oldData = YamlConfiguration.loadConfiguration(dataFile);

                    UUID uuid = UUID.fromString(dataFile.getName());
                    String name = oldData.getString("name");
                    int minutesPlayed = oldData.getInt("hoursPlayed", 0) * 60;
                    int dayNum = oldData.getInt("dayNum", 1);

                    String startDateRaw = oldData.getString("startDate");
                    LocalDate startDate = startDateRaw != null ? LocalDate.parse(startDateRaw, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null;

                    String lastCollectedDateRaw = oldData.getString("lastCollectedDate");
                    LocalDate lastCollectedDate = lastCollectedDateRaw != null ? LocalDate.parse(lastCollectedDateRaw, DateTimeFormatter.ofPattern("yyyy-MM-dd")) : null;

                    RewardUser rewardUser = new RewardUser(uuid, name, minutesPlayed);
                    rewardUser.addModuleData(new DailyRewardsModuleUserData(DailyRewardsModule.ID, dayNum, dayNum, startDate, lastCollectedDate, new HashSet<>()));

                    ActivityRewarder.getInstance().getLogger().info("Translating RewardUser data for " + name + " (" + uuid + ")");
                    try {
                        ActivityRewarder.getDataManager().saveRewardUser(rewardUser);
                        successes++;
                    } catch (Exception e) {
                        ActivityRewarder.getInstance().getLogger().severe("Failed to translate RewardUser data for " + name + " (" + uuid + ")");
                        fails++;
                    }
                }
            }

            if (successes > 0) {
                ActivityRewarder.getInstance().getLogger().info("Successfully translated RewardUser data for " + successes + " users");
            }
            if (fails > 0) {
                ActivityRewarder.getInstance().getLogger().info("Failed to translate RewardUser data for " + fails + " users");
            }

            completableFuture.complete(true);
        });

        return completableFuture;
    }
}
