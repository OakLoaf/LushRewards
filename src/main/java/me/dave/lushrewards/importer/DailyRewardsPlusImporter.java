package me.dave.lushrewards.importer;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.rewards.collections.DailyRewardCollection;
import me.dave.lushrewards.rewards.custom.ConsoleCommandReward;
import me.dave.lushrewards.rewards.custom.MessageReward;
import me.dave.lushrewards.rewards.custom.Reward;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushlib.utils.SimpleItemStack;
import org.lushplugins.lushlib.utils.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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

        LushRewards.getMorePaperLib().scheduling().asyncScheduler().run(() -> {
            YamlConfiguration drpConfig = YamlConfiguration.loadConfiguration(new File(dataFolder, "Config.yml"));
            YamlConfiguration drpRewardsConfig = YamlConfiguration.loadConfiguration(new File(dataFolder, "Rewards.yml"));

            File newConfigFile = prepareForImport(new File(LushRewards.getInstance().getDataFolder(), "config.yml"), false);
            File newRewardsFile = prepareForImport(new File(LushRewards.getInstance().getDataFolder(), "modules/daily-rewards.yml"), false);
            if (newConfigFile == null || newRewardsFile == null) {
                completableFuture.complete(false);
                return;
            }

            LushRewards.getInstance().saveResource("config.yml", true);
            LushRewards.getInstance().saveResource("modules/daily-rewards.yml", true);
            YamlConfiguration arConfig = YamlConfiguration.loadConfiguration(newConfigFile);
            YamlConfiguration arRewardsConfig = YamlConfiguration.loadConfiguration(newRewardsFile);

            if (drpConfig.getBoolean("DailyRewardReminderEnabled", false)) {
                arConfig.set("reminder-period", drpConfig.getInt("DailyRewardClaimReminder", 1800));
            } else {
                arConfig.set("reminder-period", -1);
            }

            String lineIndent = drpConfig.getString("LineIndent", "");

            if (drpConfig.contains("UnclaimedReward")) {
                drpTemplateToSimpleItemStack(drpConfig.getConfigurationSection("UnclaimedReward"), lineIndent).save(arConfig.createSection("item-templates.redeemable-reward"));
            }
            if (drpConfig.contains("RewardNotReady")) {
                drpTemplateToSimpleItemStack(drpConfig.getConfigurationSection("RewardNotReady"), lineIndent).save(arConfig.createSection("item-templates.default-reward"));
            }
            if (drpConfig.contains("MissedReward")) {
                drpTemplateToSimpleItemStack(drpConfig.getConfigurationSection("MissedReward"), lineIndent).save(arConfig.createSection("item-templates.missed-reward"));
            }
            if (drpConfig.contains("ClaimedReward")) {
                drpTemplateToSimpleItemStack(drpConfig.getConfigurationSection("ClaimedReward"), lineIndent).save(arConfig.createSection("item-templates.collected-reward"));
            }
            if (drpConfig.contains("FutureReward")) {
                drpTemplateToSimpleItemStack(drpConfig.getConfigurationSection("FutureReward"), lineIndent).save(arConfig.createSection("item-templates.upcoming-reward"));
            }
            if (drpConfig.contains("Placeholder")) {
                drpTemplateToSimpleItemStack(drpConfig.getConfigurationSection("Placeholder"), lineIndent).save(arConfig.createSection("item-templates.#"));
            }
            if (drpConfig.getBoolean("DataBar.Enabled", false)) {
                if (drpConfig.contains("DataBar.Statistics")) {
                    drpTemplateToSimpleItemStack(drpConfig.getConfigurationSection("DataBar.Statistics"), lineIndent).save(arConfig.createSection("item-templates.P"));
                }
            }

            arRewardsConfig.createSection("daily-rewards");
            AtomicInteger highestDayNum = new AtomicInteger(0);

            drpRewardsConfig.getValues(false).forEach((key, value) -> {
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
                    rewards.add(new ConsoleCommandReward(rewardSection.getStringList("RewardCommands")));
                    if (rewardSection.contains("RewardMessage")) {
                        rewards.add(new MessageReward(rewardSection.getString("RewardMessage")));
                    }

                    String displayMaterialRaw = rewardSection.getString("RewardIcon");
                    SimpleItemStack displayItem = new SimpleItemStack();
                    if (displayMaterialRaw != null && !displayMaterialRaw.isBlank()) {
                        displayItem = new SimpleItemStack(StringUtils.getEnum(displayMaterialRaw, Material.class).orElse(null));
                        if (rewardSection.getBoolean("Extras.Enchanted")) {
                            displayItem.setEnchantGlow(true);
                        }
                    }

                    if (drpConfig.getBoolean("ShowDayQuantity")) {
                        displayItem.setAmount(Math.min(dayNum, 64));
                    }

                    DailyRewardCollection rewardCollection = new DailyRewardCollection(null, null, null, dayNum, null, rewards, 0, "small", displayItem, null);
                    rewardCollection.save(arRewardsConfig.createSection("daily-rewards.day-" + dayNum));
                }

                if (drpConfig.getBoolean("ResetWhenStreakCompleted", false) && highestDayNum.get() > 0) {
                    arRewardsConfig.set("reset-days-at", highestDayNum.get());
                }

                arRewardsConfig.set("streak-mode", !drpConfig.getBoolean("PauseStreakWhenMissed", false));

                arRewardsConfig.set("default-redeem-sound", drpConfig.getString("SoundEffect", "ENTITY_PLAYER_LEVELUP"));

                arRewardsConfig.createSection("gui");
                arRewardsConfig.set("gui.title", drpConfig.getString("PluginGuiTitle", "          &#529bf2>&lDaily Rewards"));
                arRewardsConfig.set("gui.scroll-type", "GRID");
                arRewardsConfig.set("gui.template", "DAILY_REWARDS_PLUS");

                try {
                    arRewardsConfig.save(newRewardsFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    completableFuture.complete(false);
                }
            });

            try {
                arConfig.save(newConfigFile);
            } catch (IOException e) {
                e.printStackTrace();
                completableFuture.complete(false);
            }

            completableFuture.complete(true);
        });

        return completableFuture;
    }

    private SimpleItemStack drpTemplateToSimpleItemStack(ConfigurationSection configurationSection, @NotNull String lineIndent) {
        SimpleItemStack simpleItemStack = new SimpleItemStack();

        String materialRaw = configurationSection.getString("Icon");
        if (materialRaw != null && materialRaw.equalsIgnoreCase("<phead>")) {
            materialRaw = "player_head";
            simpleItemStack.setSkullTexture("mirror");
        }

        simpleItemStack.setType(StringUtils.getEnum(materialRaw, Material.class).orElse(null));

        String displayName = configurationSection.getString("Title");
        if (displayName != null) {
            displayName = translatePlaceholders(displayName);
        }
        simpleItemStack.setDisplayName(displayName);

        List<String> lore = new ArrayList<>();
        configurationSection.getStringList("Lore").forEach(line -> lore.add(translatePlaceholders(lineIndent + line)));
        simpleItemStack.setLore(lore);

        return simpleItemStack;
    }

    private String translatePlaceholders(@NotNull String string) {
        return string
            .replaceAll("<dayNum>", "%day%")
            .replaceAll("<timeUntilNextReward>", "%rewarder_countdown%")
            .replaceAll("<playerName>", "%player_name%")
            .replaceAll("<playerStreak>", "%rewarder_streak%");
    }
}
