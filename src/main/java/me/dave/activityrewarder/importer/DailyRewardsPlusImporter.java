package me.dave.activityrewarder.importer;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.rewards.collections.DailyRewardCollection;
import me.dave.activityrewarder.rewards.custom.CommandReward;
import me.dave.activityrewarder.rewards.custom.MessageReward;
import me.dave.activityrewarder.rewards.custom.Reward;
import me.dave.activityrewarder.utils.ConfigParser;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class DailyRewardsPlusImporter extends ConfigImporter {

    public DailyRewardsPlusImporter() throws FileNotFoundException {
        super();
    }

    @Override
    protected String getPluginName() {
        return "DailyRewardsPlus";
    }

    @Override
    public CompletableFuture<Boolean> startImport() {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

        ActivityRewarder.getMorePaperLib().scheduling().asyncScheduler().run(() -> {
            YamlConfiguration mainConfig = YamlConfiguration.loadConfiguration(new File(dataFolder, "Config.yml"));

            // Transfer daily rewards
            YamlConfiguration rewardsConfig = YamlConfiguration.loadConfiguration(new File(dataFolder, "Rewards.yml"));

            File rewardsFile = new File(ActivityRewarder.getInstance().getDataFolder(), "modules/daily-rewards.yml");
            if (!rewardsFile.renameTo(new File(ActivityRewarder.getInstance().getDataFolder(), "modules/daily-rewards-old-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy--HH-mm-ss")) + ".yml"))) {
                ActivityRewarder.getInstance().getLogger().severe("Failed to rename rewards file");
                completableFuture.complete(false);
                return;
            }
            File newRewardsFile = new File(ActivityRewarder.getInstance().getDataFolder(), "modules/daily-rewards.yml");
            try {
                newRewardsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                completableFuture.complete(false);
                return;
            }
            YamlConfiguration dailyRewardsConfig = YamlConfiguration.loadConfiguration(newRewardsFile);

            AtomicInteger highestDayNum = new AtomicInteger(0);
            rewardsConfig.getValues(false).forEach((key, value) -> {
                if (value instanceof ConfigurationSection rewardSection) {
                    int dayNum;
                    try {
                        dayNum = Integer.parseInt(key.replaceAll("\\D", ""));
                    } catch(NumberFormatException e) {
                        return;
                    }

                    if (dayNum > highestDayNum.get()) {
                        highestDayNum.set(dayNum);
                    }

                    Collection<Reward> rewards = new ArrayList<>();
                    rewardSection.getStringList("RewardCommands").forEach(command -> rewards.add(new CommandReward(command)));
                    if (rewardSection.contains("RewardMessage")) {
                        rewards.add(new MessageReward(rewardSection.getString("RewardMessage")));
                    }

                    String displayMaterial = rewardSection.getString("RewardIcon");
                    SimpleItemStack displayItem = new SimpleItemStack();
                    if (displayMaterial != null && !displayMaterial.isBlank()) {
                        displayItem = new SimpleItemStack(ConfigParser.getMaterial(displayMaterial));
                        if (rewardSection.getBoolean("Extras.Enchanted")) {
                            displayItem.setEnchanted(true);
                        }
                    }
                    displayItem.setAmount(Math.min(dayNum, 64));

                    DailyRewardCollection rewardCollection = new DailyRewardCollection(null, null, null, dayNum, null, rewards, 0, "small", displayItem, null);
                    rewardCollection.save(dailyRewardsConfig.createSection("daily-rewards.day-" + dayNum));
                }

                if (highestDayNum.get() > 0) {
                    dailyRewardsConfig.set("reset-days-at", highestDayNum.get());
                }

                dailyRewardsConfig.set("gui.title", "lala");
                dailyRewardsConfig.set("gui.scroll-type", "GRID");
                dailyRewardsConfig.set("gui.template", "DAILY_REWARDS_PLUS");

                try {
                    dailyRewardsConfig.save(newRewardsFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    completableFuture.complete(false);
                }
            });

            completableFuture.complete(true);
        });

        return completableFuture;
    }
}
