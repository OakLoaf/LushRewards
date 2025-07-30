package org.lushplugins.lushrewards.migrator.importer;

import org.lushplugins.lushlib.utils.DisplayItemStack;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.reward.module.dailyrewards.DailyRewardCollection;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;
import org.lushplugins.lushlib.utils.SimpleItemStack;
import org.lushplugins.lushlib.utils.StringUtils;
import org.lushplugins.rewardsapi.api.reward.Reward;
import org.lushplugins.rewardsapi.api.reward.type.ConsoleCommandReward;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DailyRewardsPlusImporter extends ConfigImporter {

    public DailyRewardsPlusImporter() throws FileNotFoundException {
        super("DailyRewardsPlus");
    }

    @Override
    public boolean startImport() {
        YamlConfiguration importingConfig = YamlConfiguration.loadConfiguration(new File(this.getDataFolder(), "Config.yml"));
        YamlConfiguration importingRewardsConfig = YamlConfiguration.loadConfiguration(new File(this.getDataFolder(), "Rewards.yml"));

        File newConfigFile = prepareForImport(new File(LushRewards.getInstance().getDataFolder(), "config.yml"), false);
        File newRewardsFile = prepareForImport(new File(LushRewards.getInstance().getDataFolder(), "modules/daily-rewards.yml"), false);
        if (newConfigFile == null || newRewardsFile == null) {
            return false;
        }
        LushRewards.getInstance().saveResource("config.yml", true);
        LushRewards.getInstance().saveResource("modules/daily-rewards.yml", true);
        YamlConfiguration localConfig = YamlConfiguration.loadConfiguration(newConfigFile);
        YamlConfiguration localRewardsConfig = YamlConfiguration.loadConfiguration(newRewardsFile);

        if (importingConfig.getBoolean("DailyRewardReminderEnabled", false)) {
            localConfig.set("reminder-period", importingConfig.getInt("DailyRewardClaimReminder", 1800));
        } else {
            localConfig.set("reminder-period", -1);
        }

        String lineIndent = importingConfig.getString("LineIndent", "");

        if (importingConfig.contains("UnclaimedReward")) {
            importingTemplateToSimpleItemStack(importingConfig.getConfigurationSection("UnclaimedReward"), lineIndent).save(localRewardsConfig.createSection("gui.item-templates.redeemable-reward"));
        }
        if (importingConfig.contains("RewardNotReady")) {
            importingTemplateToSimpleItemStack(importingConfig.getConfigurationSection("RewardNotReady"), lineIndent).save(localRewardsConfig.createSection("gui.item-templates.default-reward"));
        }
        if (importingConfig.contains("MissedReward")) {
            importingTemplateToSimpleItemStack(importingConfig.getConfigurationSection("MissedReward"), lineIndent).save(localRewardsConfig.createSection("gui.item-templates.missed-reward"));
        }
        if (importingConfig.contains("ClaimedReward")) {
            importingTemplateToSimpleItemStack(importingConfig.getConfigurationSection("ClaimedReward"), lineIndent).save(localRewardsConfig.createSection("gui.item-templates.collected-reward"));
        }
        if (importingConfig.contains("FutureReward")) {
            importingTemplateToSimpleItemStack(importingConfig.getConfigurationSection("FutureReward"), lineIndent).save(localRewardsConfig.createSection("gui.item-templates.upcoming-reward"));
        }
        if (importingConfig.contains("Placeholder")) {
            importingTemplateToSimpleItemStack(importingConfig.getConfigurationSection("Placeholder"), lineIndent).save(localRewardsConfig.createSection("gui.item-templates.#"));
        }
        if (importingConfig.getBoolean("DataBar.Enabled", false)) {
            if (importingConfig.contains("DataBar.Statistics")) {
                importingTemplateToSimpleItemStack(importingConfig.getConfigurationSection("DataBar.Statistics"), lineIndent).save(localRewardsConfig.createSection("gui.item-templates.P"));
            }
        }

        localRewardsConfig.createSection("daily-rewards");
        AtomicInteger highestDayNum = new AtomicInteger(0);

        importingRewardsConfig.getValues(false).forEach((key, value) -> {
            if (value instanceof ConfigurationSection rewardSection) {
                int dayNum;
                try {
                    dayNum = Integer.parseInt(key.replaceAll("\\D", ""));
                } catch (NumberFormatException e) {
                    return;
                }

                if (dayNum > highestDayNum.get()) {
                    highestDayNum.set(dayNum);
                }

                Collection<Reward> rewards = new ArrayList<>();
                Reward reward = new ConsoleCommandReward(rewardSection.getStringList("RewardCommands"));
                if (rewardSection.contains("RewardMessage")) {
                    reward.setMessage(rewardSection.getString("RewardMessage"));
                }
                rewards.add(reward);

                String displayMaterialRaw = rewardSection.getString("RewardIcon");
                DisplayItemStack.Builder displayItemBuilder;
                if (displayMaterialRaw != null && !displayMaterialRaw.isBlank()) {
                    displayItemBuilder = DisplayItemStack.builder(StringUtils.getEnum(displayMaterialRaw, Material.class).orElse(null));
                    if (rewardSection.getBoolean("Extras.Enchanted")) {
                        displayItemBuilder.setEnchantGlow(true);
                    }
                } else {
                    displayItemBuilder = DisplayItemStack.builder();
                }

                if (importingConfig.getBoolean("ShowDayQuantity")) {
                    displayItemBuilder.setAmount(Math.min(dayNum, 64));
                }

                DailyRewardCollection rewardCollection = new DailyRewardCollection(null, null, null, dayNum, null, rewards, 0, "small", displayItemBuilder.build(), null);
                rewardCollection.save(localRewardsConfig.createSection("daily-rewards.day-" + dayNum));
            }
        });

        if (importingConfig.getBoolean("ResetWhenStreakCompleted", false) && highestDayNum.get() > 0) {
            localRewardsConfig.set("reset-days-at", highestDayNum.get());
        }

        localRewardsConfig.set("reward-mode", importingConfig.getBoolean("PauseStreakWhenMissed", false) ? "default" : "streak");

        localRewardsConfig.set("default-redeem-sound", importingConfig.getString("SoundEffect", "ENTITY_PLAYER_LEVELUP"));

        localRewardsConfig.createSection("gui");
        localRewardsConfig.set("gui.title", importingConfig.getString("PluginGuiTitle", "          &#529bf2>&lDaily Rewards"));
        localRewardsConfig.set("gui.scroll-type", "GRID");
        localRewardsConfig.set("gui.template", "DAILY_REWARDS_PLUS");

        try {
            localConfig.save(newConfigFile);
            localRewardsConfig.save(newRewardsFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private SimpleItemStack importingTemplateToSimpleItemStack(ConfigurationSection configurationSection, @NotNull String lineIndent) {
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
            .replace("<dayNum>", "%day%")
            .replace("<timeUntilNextReward>", "%lushrewards_countdown%")
            .replace("<playerName>", "%player_name%")
            .replace("<playerStreak>", "%lushrewards_daily-rewards_streak%");
    }
}
