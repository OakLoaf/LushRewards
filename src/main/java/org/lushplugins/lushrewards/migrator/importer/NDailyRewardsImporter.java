package org.lushplugins.lushrewards.migrator.importer;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.lushplugins.lushlib.utils.DisplayItemStack;
import org.lushplugins.lushlib.utils.SimpleItemStack;
import org.lushplugins.lushlib.utils.StringUtils;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.gui.GuiFormat;
import org.lushplugins.lushrewards.reward.module.dailyrewards.DailyRewardCollection;
import org.lushplugins.rewardsapi.api.reward.Reward;
import org.lushplugins.rewardsapi.api.reward.type.ConsoleCommandReward;
import org.lushplugins.rewardsapi.api.reward.type.PlayerCommandReward;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class NDailyRewardsImporter extends ConfigImporter {

    public NDailyRewardsImporter() throws FileNotFoundException {
        super("NDailyRewards");
    }

    @Override
    public boolean startImport() {
        YamlConfiguration importingConfig = YamlConfiguration.loadConfiguration(new File(this.getDataFolder(), "config.yml"));

        File newRewardsFile = prepareForImport(new File(LushRewards.getInstance().getDataFolder(), "modules/daily-rewards.yml"), false);
        if (newRewardsFile == null) {
            return false;
        }
        LushRewards.getInstance().saveResource("modules/daily-rewards.yml", true);
        YamlConfiguration localRewardsConfig = YamlConfiguration.loadConfiguration(newRewardsFile);

        localRewardsConfig.set("reset-days-at", importingConfig.getInt("days-row", -1));

        ConfigurationSection importingRewardsSection = importingConfig.getConfigurationSection("rewards");
        if (importingRewardsSection != null) {
            importingRewardsSection.getValues(false).forEach((dayNumRaw, value) -> {
                if (value instanceof ConfigurationSection rewardSection) {
                    int dayNum = Integer.parseInt(dayNumRaw);

                    Collection<Reward> rewards = new ArrayList<>();

                    List<String> commands = rewardSection.getStringList("commands");
                    List<String> consoleCommands = new ArrayList<>();
                    List<String> playerCommands = new ArrayList<>();
                    commands.forEach(command -> {
                        if (command.startsWith("console:")) {
                            consoleCommands.add(command.replace("console:", "").trim());
                        } else {
                            playerCommands.add(command);
                        }
                    });

                    if (!consoleCommands.isEmpty()) {
                        rewards.add(new ConsoleCommandReward(consoleCommands));
                    }
                    if (!playerCommands.isEmpty()) {
                        rewards.add(new PlayerCommandReward(playerCommands));
                    }

                    List<String> messages = rewardSection.getStringList("messages");
                    if (messages.size() > 1) {
                        rewards.forEach(reward -> reward.setMessage(messages.get(0)));
                    }

                    DisplayItemStack displayItem = DisplayItemStack.builder()
                        .setLore(rewardSection.getStringList("lore"))
                        .build();

                    DailyRewardCollection rewardCollection = new DailyRewardCollection(null, null, null, dayNum, null, rewards, 0, "small", displayItem, null);
                    rewardCollection.save(localRewardsConfig.createSection("daily-rewards.day-" + dayNum));
                }
            });
        }

        ConfigurationSection importingGuiSection = importingConfig.getConfigurationSection("gui");
        if (importingGuiSection != null) {
            localRewardsConfig.set("gui.title", importingGuiSection.getString("title"));

            if (importingGuiSection.contains("days-display.locked")) {
                importingTemplateToSimpleItemStack(importingGuiSection.getConfigurationSection("days-display.locked")).save(localRewardsConfig.createSection("gui.item-templates.default-reward"));
            }
            if (importingGuiSection.contains("days-display.available")) {
                importingTemplateToSimpleItemStack(importingGuiSection.getConfigurationSection("days-display.available")).save(localRewardsConfig.createSection("gui.item-templates.redeemable-reward"));
            }
            if (importingGuiSection.contains("days-display.taken")) {
                importingTemplateToSimpleItemStack(importingGuiSection.getConfigurationSection("days-display.taken")).save(localRewardsConfig.createSection("gui.item-templates.collected-reward"));
            }
            if (importingGuiSection.contains("days-display.next")) {
                importingTemplateToSimpleItemStack(importingGuiSection.getConfigurationSection("days-display.next")).save(localRewardsConfig.createSection("gui.item-templates.upcoming-reward"));
            }
            if (importingGuiSection.contains("items.panes")) {
                importingTemplateToSimpleItemStack(importingGuiSection.getConfigurationSection("items.panes")).save(localRewardsConfig.createSection("gui.item-templates.#"));
            }

            localRewardsConfig.set("gui.template", "CUSTOM");
            localRewardsConfig.set("gui.format", GuiFormat.GuiTemplate.NDAILY_REWARDS.getRows());
        }

        try {
            localRewardsConfig.save(newRewardsFile);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private SimpleItemStack importingTemplateToSimpleItemStack(ConfigurationSection configurationSection) {
        SimpleItemStack simpleItemStack = new SimpleItemStack();

        String[] itemDataRaw = configurationSection.getString("material", "stone").split(":");
        String materialRaw = itemDataRaw[0];
        if (itemDataRaw.length >= 3) {
            simpleItemStack.setAmount(Integer.parseInt(itemDataRaw[2]));
        }

        simpleItemStack.setType(StringUtils.getEnum(materialRaw, Material.class).orElse(null));
        simpleItemStack.setDisplayName(configurationSection.getString("name"));
        simpleItemStack.setLore(configurationSection.getStringList("lore"));

        if (materialRaw.equalsIgnoreCase("player_head")) {
            simpleItemStack.setSkullTexture(configurationSection.getString("player-head-texture", "mirror"));
        }

        if (configurationSection.contains("custom-model-data")) {
            simpleItemStack.setCustomModelData(configurationSection.getInt("custom-model-data"));
        }

        if (configurationSection.contains("enchanted")) {
            simpleItemStack.setEnchantGlow(configurationSection.getBoolean("enchanted"));
        }

        return simpleItemStack;
    }
}
