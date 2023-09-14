package me.dave.activityrewarder.importer;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.rewards.collections.DailyRewardCollection;
import me.dave.activityrewarder.rewards.custom.ConsoleCommandReward;
import me.dave.activityrewarder.rewards.custom.ItemReward;
import me.dave.activityrewarder.rewards.custom.Reward;
import me.dave.activityrewarder.utils.ConfigParser;
import me.dave.activityrewarder.utils.SimpleItemStack;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class ActivityRewarderConfigUpdater extends ConfigImporter {

    public ActivityRewarderConfigUpdater() throws FileNotFoundException {
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
            YamlConfiguration oldConfig = YamlConfiguration.loadConfiguration(new File(ActivityRewarder.getInstance().getDataFolder(), "config.yml"));

            File newConfigFile = prepareForImport(new File(ActivityRewarder.getInstance().getDataFolder(), "config.yml"), false);
            File newRewardsFile = prepareForImport(new File(ActivityRewarder.getInstance().getDataFolder(), "modules/daily-rewards.yml"), false);
            if (newConfigFile == null || newRewardsFile == null) {
                completableFuture.complete(false);
                return;
            }

            ActivityRewarder.getInstance().saveResource("config.yml", true);
            ActivityRewarder.getInstance().saveResource("modules/daily-rewards.yml", true);
            YamlConfiguration arConfig = YamlConfiguration.loadConfiguration(newConfigFile);
            YamlConfiguration arRewardsConfig = YamlConfiguration.loadConfiguration(newRewardsFile);

            // Changes to config.yml
            if (oldConfig.contains("reminder-period")) {
                arConfig.set("reminder-period", oldConfig.get("reminder-period"));
            }

            ConfigurationSection sizesSection = oldConfig.getConfigurationSection("sizes");
            if (sizesSection != null) {
                arConfig.createSection("categories");
                sizesSection.getValues(false).forEach((size, materialName) -> arConfig.set("categories." + size + ".material", materialName));
            }

            if (oldConfig.contains("gui.redeemable-name")) {
                arConfig.set("item-templates.redeemable-reward.display-name", oldConfig.getString("gui.redeemable-name"));
            }
            if (oldConfig.contains("gui.collected-item")) {
                arConfig.set("item-templates.collected-reward.material", oldConfig.getString("gui.collected-item"));
                arConfig.set("item-templates.collected-reward.skull-texture", null);
            }
            if (oldConfig.contains("gui.collected-name")) {
                arConfig.set("item-templates.collected-reward.display-name", oldConfig.getString("gui.collected-name"));
            }
            if (oldConfig.contains("gui.border-item")) {
                arConfig.set("item-templates.#.material", oldConfig.getString("gui.border-item"));
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
                arRewardsConfig.set("gui.template", oldConfig.getString("gui.template"));
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
                        rewardSection.getStringList("rewards.commands").forEach(command -> rewards.add(new ConsoleCommandReward(command)));

                        ConfigurationSection itemRewardsSection = rewardSection.getConfigurationSection("rewards.items");
                        if (itemRewardsSection != null) {
                            itemRewardsSection.getValues(false).forEach((materialRaw, data) -> {
                                Material material = ConfigParser.getMaterial(materialRaw);
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
