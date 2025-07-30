package org.lushplugins.lushrewards.reward.module.dailyrewards;

import com.google.common.collect.TreeMultimap;
import org.bukkit.inventory.Inventory;
import org.lushplugins.lushlib.utils.DisplayItemStack;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.config.ConfigManager;
import org.lushplugins.lushrewards.gui.GuiFormat;
import org.lushplugins.lushrewards.reward.module.playtimerewards.PlaytimeRewardsModule;
import org.lushplugins.lushrewards.reward.RewardDay;
import org.lushplugins.lushrewards.utils.Debugger;
import org.lushplugins.lushlib.gui.inventory.Gui;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.lushplugins.rewardsapi.api.RewardsAPI;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DailyRewardsGui extends Gui {
    private final DailyRewardsModule module;
    private final GuiFormat.GuiTemplate guiTemplate;

    public DailyRewardsGui(DailyRewardsModule module, Player player) {
        super(module.getGuiFormat().getTemplate().getRowCount() * 9, ChatColorHandler.translate(module.getGuiFormat().getTitle()), player);
        this.guiTemplate = module.getGuiFormat().getTemplate();
        this.module = module;
    }

    @Override
    public void refresh() {
        Inventory inventory = this.getInventory();
        Player player = this.getPlayer();

        inventory.clear();
        clearButtons();

        module.getOrLoadUserData(player.getUniqueId(), true)
            .completeOnTimeout(null, 15, TimeUnit.SECONDS)
            .thenAccept(userData -> RewardsAPI.getMorePaperLib().scheduling().globalRegionalScheduler().run(() -> {
                if (userData == null) {
                    DisplayItemStack errorItem = DisplayItemStack.builder(Material.BARRIER)
                        .setDisplayName("&#ff6969Failed to load rewards user data try relogging")
                        .setLore(List.of("&7&oIf this continues please", "&7&oreport to your server administrator"))
                        .build();

                    inventory.setItem(4, errorItem.asItemStack(player, true));
                    return;
                }

                module.checkRewardDay(userData);

                boolean collectedToday = userData.hasCollectedToday();
                int currDayNum = userData.getDayNum();

                // First reward day shown
                AtomicInteger dayIndex = new AtomicInteger();
                final LocalDate[] dateIndex = new LocalDate[1];
                switch (module.getScrollType()) {
                    case GRID -> {
                        int rewardDisplaySize = guiTemplate.countChar('R');

                        int endDay = rewardDisplaySize;
                        if (rewardDisplaySize >= 1) {
                            while (currDayNum > endDay) {
                                endDay += rewardDisplaySize;
                            }
                        }

                        dayIndex.set(endDay - (rewardDisplaySize - 1));

                        int diff = dayIndex.get() - currDayNum;
                        dateIndex[0] = LocalDate.now().plusDays(diff);
                    }
                    case MONTH -> {
                        LocalDate today = LocalDate.now();
                        dateIndex[0] = LocalDate.of(today.getYear(), today.getMonth(), 1);
                        dayIndex.set(currDayNum - (today.getDayOfMonth() - 1));
                    }
                    default -> {
                        dayIndex.set(currDayNum);
                        dateIndex[0] = LocalDate.now();
                    }
                }

                ConfigManager configManager = LushRewards.getInstance().getConfigManager();
                TreeMultimap<Character, Integer> slotMap = guiTemplate.getSlotMap();
                HashSet<Integer> collectedDays = userData.getCollectedDays();
                for (Character character : slotMap.keySet()) {
                    switch (character) {
                        case 'R' -> slotMap.get(character).forEach(slot -> {
                            ItemStack itemStack;
                            if (module.getScrollType().equals(ScrollType.MONTH) && dateIndex[0].getMonthValue() != LocalDate.now().getMonthValue()) {
                                DisplayItemStack.Builder displayItemBuilder = DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getItemTemplate(String.valueOf('#'), module));

                                if (!displayItemBuilder.hasType()) {
                                    displayItemBuilder.setType(Material.STONE);
                                    LushRewards.getInstance().getLogger().severe("Failed to display custom item-template '#' as it does not specify a valid material");
                                }

                                itemStack = displayItemBuilder.build().asItemStack(player, true);
                            } else {
                                // Get the day's reward for the current slot
                                RewardDay rewardDay = module.getRewardDay(dateIndex[0], dayIndex.get());
                                DailyRewardCollection priorityReward = rewardDay.getHighestPriorityRewardCollection();

                                String itemTemplate;
                                if (dayIndex.get() < currDayNum) {
                                    itemTemplate = (collectedDays.contains(dayIndex.get())) ? "collected-reward" : "missed-reward";
                                } else if (dayIndex.get() == currDayNum) {
                                    itemTemplate = collectedToday ? "collected-reward" : "redeemable-reward";
                                } else {
                                    itemTemplate = "default-reward";
                                }

                                DisplayItemStack.Builder displayItemBuilder;
                                if (!rewardDay.isEmpty()) {
                                    displayItemBuilder = DisplayItemStack.builder(configManager.getCategoryTemplate(priorityReward.getCategory())).overwrite(
                                        DisplayItemStack.builder(configManager.getItemTemplate(itemTemplate, module)),
                                        DisplayItemStack.builder(priorityReward.getDisplayItem()));

                                    if (displayItemBuilder.getDisplayName() != null) {
                                        displayItemBuilder.setDisplayName(displayItemBuilder.getDisplayName()
                                            .replace("%claimed%", itemTemplate.equals("collected-reward") ? module.getRewardPlaceholderClaimed() : module.getRewardPlaceholderUnclaimed())
                                            .replace("%day%", String.valueOf(dayIndex.get()))
                                            .replace("%month_day%", String.valueOf(dateIndex[0].getDayOfMonth()))
                                            .replace("%month%", dateIndex[0].getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                                            .replace("%month_num%", String.valueOf(dateIndex[0].getMonthValue()))
                                            .replace("%year%", String.valueOf(dateIndex[0].getYear()))
                                            .replace("%date%", dateIndex[0].format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                            .replace("%date_us%", dateIndex[0].format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))));
                                    }

                                    if (displayItemBuilder.getLore() != null) {
                                        displayItemBuilder.setLore(displayItemBuilder.getLore().stream().map(line ->
                                            line.replace("%claimed%", itemTemplate.equals("collected-reward") ? module.getRewardPlaceholderClaimed() : module.getRewardPlaceholderUnclaimed())
                                                .replace("%day%", String.valueOf(dayIndex.get()))
                                                .replace("%month_day%", String.valueOf(dateIndex[0].getDayOfMonth()))
                                                .replace("%month%", dateIndex[0].getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                                                .replace("%month_num%", String.valueOf(dateIndex[0].getMonthValue()))
                                                .replace("%year%", String.valueOf(dateIndex[0].getYear()))
                                                .replace("%date%", dateIndex[0].format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                                .replace("%date_us%", dateIndex[0].format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
                                        ).toList());
                                    }

                                    displayItemBuilder.parseColors(player);
                                } else {
                                    displayItemBuilder = DisplayItemStack.builder(Material.AIR);
                                }

                                try {
                                    itemStack = displayItemBuilder.build().asItemStack(player);
                                } catch (IllegalArgumentException e) {
                                    LushRewards.getInstance().getLogger().severe("Failed to display item-template '" + itemTemplate + "' as it does not specify a valid material");
                                    itemStack = new ItemStack(Material.STONE);
                                }

                                // Changes item data based on if the reward has been collected or not
                                if (dayIndex.get() == currDayNum && !collectedToday) {
                                    addButton(slot, (event) -> {
                                        // Gets clicked item and checks if it exists
                                        ItemStack currItem = event.getCurrentItem();
                                        if (currItem == null) {
                                            return;
                                        }

                                        removeButton(slot);

                                        Debugger.sendDebugMessage("Starting reward process for " + player.getName(), Debugger.DebugMode.ALL);

                                        module.claimRewards(player);

                                        LushRewards.getInstance().getRewardModuleManager().getModules(PlaytimeRewardsModule.class).forEach(module -> {
                                            if (module.shouldReceiveWithDailyRewards()) {
                                                module.claimRewards(player);
                                            }
                                        });

                                        DisplayItemStack.Builder collectedItemBuilder = DisplayItemStack.builder(currItem).overwrite(
                                            DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getItemTemplate("collected-reward", module)));

                                        if (collectedItemBuilder.getDisplayName() != null) {
                                            collectedItemBuilder.setDisplayName(collectedItemBuilder.getDisplayName()
                                                .replace("%claimed%", itemTemplate.equals("collected-reward") ? module.getRewardPlaceholderClaimed() : module.getRewardPlaceholderUnclaimed())
                                                .replace("%day%", String.valueOf(currDayNum))
                                                .replace("%month_day%", String.valueOf(dateIndex[0].getDayOfMonth()))
                                                .replace("%month%", dateIndex[0].getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                                                .replace("%month_num%", String.valueOf(dateIndex[0].getMonthValue()))
                                                .replace("%year%", String.valueOf(dateIndex[0].getYear()))
                                                .replace("%date%", dateIndex[0].format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                                .replace("%date_us%", dateIndex[0].format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))));
                                        }
                                        collectedItemBuilder.parseColors(player);

                                        inventory.setItem(slot, collectedItemBuilder.build().asItemStack(player, true));
                                    });
                                }

                                // Sets the size of the stack to the same amount as the current date
                                if (module.showDateAsAmount()) {
                                    itemStack.setAmount(dateIndex[0].getDayOfMonth());
                                }
                            }

                            inventory.setItem(slot, itemStack);

                            dayIndex.getAndIncrement();
                            dateIndex[0] = dateIndex[0].plusDays(1);
                        });
                        case 'U', 'N' -> {
                            String upcomingCategory = module.getUpcomingCategory();
                            Optional<DailyRewardCollection> upcomingReward = module.findNextRewardFromCategory(dayIndex.get(), dateIndex[0], upcomingCategory);

                            // Adds the upcoming reward to the GUI if it exists
                            if (upcomingReward.isPresent()) {
                                DailyRewardCollection upcomingRewardCollection = upcomingReward.get();

                                // Get the day's reward for the current slot
                                DisplayItemStack.Builder categoryItemBuilder = DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getCategoryTemplate(upcomingCategory)).overwrite(
                                    DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getItemTemplate("upcoming-reward", module)),
                                    DisplayItemStack.builder(upcomingRewardCollection.getDisplayItem()));

                                if (categoryItemBuilder.getDisplayName() != null) {
                                    categoryItemBuilder.setDisplayName(ChatColorHandler.translate(categoryItemBuilder
                                            .getDisplayName()
                                            .replace("%day%", String.valueOf(upcomingRewardCollection.getRewardDayNum()))
                                            .replace("%month_day%", String.valueOf(dateIndex[0].getDayOfMonth()))
                                            .replace("%month%", dateIndex[0].getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                                            .replace("%month_num%", String.valueOf(dateIndex[0].getMonthValue()))
                                            .replace("%year%", String.valueOf(dateIndex[0].getYear()))
                                            .replace("%date%", dateIndex[0].format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                            .replace("%date_us%", dateIndex[0].format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))),
                                        player));
                                }

                                if (categoryItemBuilder.getLore() != null) {
                                    categoryItemBuilder.setLore(categoryItemBuilder.getLore().stream().map(line -> ChatColorHandler.translate(line, player)).toList());
                                }

                                ItemStack itemStack = categoryItemBuilder.build().asItemStack(player);
                                slotMap.get(character).forEach(slot -> inventory.setItem(slot, itemStack));
                            }
                        }
                        case ' ' ->
                            slotMap.get(character).forEach(slot -> inventory.setItem(slot, new ItemStack(Material.AIR)));
                        default -> slotMap.get(character).forEach(slot -> {
                            DisplayItemStack item = LushRewards.getInstance().getConfigManager().getItemTemplate(String.valueOf(character), module);

                            if (!item.hasType()) {
                                item = DisplayItemStack.builder(item)
                                    .setType(Material.RED_STAINED_GLASS_PANE)
                                    .build();

                                LushRewards.getInstance().getLogger().severe("Failed to display custom item-template '" + character + "' as it does not specify a valid material");
                            }

                            inventory.setItem(slot, item.asItemStack(player, true));
                        });
                    }
                }
            }));
    }

    @Override
    public void onClick(InventoryClickEvent event) {
        super.onClick(event, true);
    }

    public enum ScrollType {
        DAY,
        MONTH,
        GRID
    }
}