package me.dave.lushrewards.importer.internal;

import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.importer.ConfigImporter;
import me.dave.lushrewards.rewards.collections.DailyRewardCollection;
import me.dave.lushrewards.rewards.custom.ConsoleCommandReward;
import me.dave.lushrewards.rewards.custom.ItemReward;
import me.dave.lushrewards.rewards.custom.Reward;
import me.dave.platyutils.PlatyUtils;
import me.dave.platyutils.utils.SimpleItemStack;
import me.dave.platyutils.utils.StringUtils;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class LushRewardsConfigUpdater extends ConfigImporter {

    public LushRewardsConfigUpdater() throws FileNotFoundException {
        super();
    }

    @Override
    protected String getPluginName() {
        return "LushRewards";
    }

    @Override
    public CompletableFuture<Boolean> startImport() {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

        PlatyUtils.getMorePaperLib().scheduling().asyncScheduler().run(() -> {
            YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(new File(LushRewards.getInstance().getDataFolder(), "config.yml"));

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

            // Changes to config.yml
            if (oldConfig.contains("reminder-period")) {
                arConfig.set("reminder-period", oldConfig.get("reminder-period"));
            }

            ConfigurationSection sizesSection = oldConfig.getConfigurationSection("sizes");
            if (sizesSection != null) {
                arConfig.createSection("categories");
                sizesSection.getValues(false).forEach((size, sizeData) -> {
                    String[] sizeDataArr = ((String) sizeData).split(";");
                    String materialName = sizeDataArr[0];
                    arConfig.set("categories." + size + ".material", materialName);

                    if (sizeDataArr.length >= 2) {
                        arConfig.set("categories." + size + ".custom-model-data", sizeDataArr[1]);
                    }
                });
            }

            arConfig.set("item-templates.redeemable-reward.material", null);
            arConfig.set("item-templates.redeemable-reward.skull-texture", null);

            arConfig.set("item-templates.missed-reward.material", null);
            arConfig.set("item-templates.missed-reward.skull-texture", null);

            if (oldConfig.contains("gui.redeemable-name")) {
                arConfig.set("item-templates.default-reward.display-name", oldConfig.getString("gui.redeemable-name"));
                arConfig.set("item-templates.redeemable-reward.display-name", oldConfig.getString("gui.redeemable-name"));
                arConfig.set("item-templates.missed-reward.display-name", oldConfig.getString("gui.redeemable-name") + " &8(Missed)");
                arConfig.set("item-templates.upcoming-reward.display-name", oldConfig.getString("gui.redeemable-name") + " &8(Upcoming)");
            }
            if (oldConfig.contains("gui.collected-item")) {
                String[] sizeDataArr = oldConfig.getString("gui.collected-item", "").split(";");
                String materialName = sizeDataArr[0];
                String customModelData = sizeDataArr.length >= 2 ? sizeDataArr[1] : null;

                arConfig.set("item-templates.collected-reward.material", materialName);
                arConfig.set("item-templates.collected-reward.custom-model-data", customModelData);
                arConfig.set("item-templates.collected-reward.skull-texture", null);
            }
            if (oldConfig.contains("gui.collected-name")) {
                arConfig.set("item-templates.collected-reward.display-name", oldConfig.getString("gui.collected-name"));
            }
            if (oldConfig.contains("gui.border-item")) {
                String[] sizeDataArr = oldConfig.getString("gui.border-item", "").split(";");
                String materialName = sizeDataArr[0];
                String customModelData = sizeDataArr.length >= 2 ? sizeDataArr[1] : null;

                arConfig.set("item-templates.#.material", materialName);
                arConfig.set("item-templates.#.custom-model-data", customModelData);
            }

            if (oldConfig.getBoolean("gui.upcoming-reward.enabled", false) && oldConfig.contains("gui.upcoming-reward.lore")) {
                arConfig.set("item-templates.upcoming-reward.lore", oldConfig.getStringList("gui.upcoming-reward.lore"));
            }

            arConfig.set("debug-mode", oldConfig.getString("debug-mode", "none"));

            // Changes to daily-rewards.yml
            if (oldConfig.contains("days-reset")) {
                arRewardsConfig.set("streak-mode", oldConfig.getBoolean("days-reset", false));
            }

            if (oldConfig.contains("gui.title")) {
                arRewardsConfig.set("gui.title", oldConfig.getString("gui.title"));
            }
            if (oldConfig.contains("gui.template")) {
                String oldTemplate = oldConfig.getString("gui.template", "DEFAULT");
                if (oldTemplate.equalsIgnoreCase("DEFAULT")) {
                    arRewardsConfig.set("gui.template", "CUSTOM");
                    arRewardsConfig.set("gui.format", List.of(
                        "#########",
                        "RRRRRRR#U",
                        "#########"
                    ));
                } else {
                    arRewardsConfig.set("gui.template", oldTemplate);
                }
            }
            if (oldConfig.contains("gui.format")) {
                arRewardsConfig.set("gui.format", oldConfig.getStringList("gui.format"));
            }

            arRewardsConfig.createSection("daily-rewards");
            ConfigurationSection rewardsSection = oldConfig.getConfigurationSection("reward-days");
            if (rewardsSection != null) {
                rewardsSection.getValues(false).forEach((key, value) -> {
                    if (value instanceof ConfigurationSection rewardSection) {
                        Integer repeatFrequency = null;
                        LocalDate rewardDate = null;
                        int priority = 0;
                        if (key.equalsIgnoreCase("default")) {
                            repeatFrequency = 1;
                            rewardDate = LocalDate.of(1982, 10, 1);
                            priority = -1;
                        }

                        String category = rewardSection.getString("size");

                        Integer dayNum = null;
                        try {
                            dayNum = Integer.parseInt(key.replaceAll("\\D", ""));
                        } catch(NumberFormatException ignored) {}

                        Collection<Reward> rewards = new ArrayList<>();
                        rewards.add(new ConsoleCommandReward(rewardSection.getStringList("rewards.commands")));

                        ConfigurationSection itemRewardsSection = rewardSection.getConfigurationSection("rewards.items");
                        if (itemRewardsSection != null) {
                            itemRewardsSection.getValues(false).forEach((materialRaw, data) -> {
                                Material material = StringUtils.getEnum(materialRaw, Material.class).orElse(null);
                                int amount = 1;

                                if (data instanceof ConfigurationSection dataSection) {
                                    amount = dataSection.getInt("amount", 1);
                                }

                                rewards.add(new ItemReward(new SimpleItemStack(material, amount)));
                            });
                        }

                        SimpleItemStack displayItem = new SimpleItemStack();
                        if (rewardSection.contains("lore")) {
                            displayItem.setLore(rewardSection.getStringList("lore"));
                        }

                        DailyRewardCollection rewardCollection = new DailyRewardCollection(repeatFrequency, rewardDate, null, dayNum, null, rewards, priority, category, displayItem, null);

                        if (dayNum != null) {
                            rewardCollection.save(arRewardsConfig.createSection("daily-rewards.day-" + dayNum));
                        } else {
                            rewardCollection.save(arRewardsConfig.createSection("daily-rewards.default"));
                        }
                    }
                });
            }

            try {
                arConfig.save(newConfigFile);
                arRewardsConfig.save(newRewardsFile);
            } catch (IOException e) {
                e.printStackTrace();
                completableFuture.complete(false);
            }

            completableFuture.complete(true);
        });

        return completableFuture;
    }
}
