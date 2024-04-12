package me.dave.lushrewards.module.dailyrewards;

import com.google.common.collect.TreeMultimap;
import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.gui.GuiFormat;
import me.dave.lushrewards.module.playtimerewards.PlaytimeRewardsModule;
import me.dave.lushrewards.rewards.collections.DailyRewardCollection;
import me.dave.lushrewards.rewards.collections.RewardDay;
import me.dave.lushrewards.utils.Debugger;
import me.dave.platyutils.gui.inventory.Gui;
import me.dave.platyutils.libraries.chatcolor.ChatColorHandler;
import me.dave.platyutils.utils.SimpleItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

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
    public void recalculateContents() {
        inventory.clear();
        clearButtons();

        module.getOrLoadUserData(player.getUniqueId(), true)
            .completeOnTimeout(null, 15, TimeUnit.SECONDS)
            .thenAccept(userData -> Bukkit.getScheduler().runTask(LushRewards.getInstance(), () -> {
                if (userData == null) {
                    SimpleItemStack errorItem = new SimpleItemStack(Material.BARRIER);
                    errorItem.setDisplayName("&#ff6969Failed to load rewards user data try relogging");
                    errorItem.setLore(List.of("&7&oIf this continues please", "&7&oreport to your server administrator"));
                    errorItem.parseColors(player);

                    inventory.setItem(4, errorItem.asItemStack());

                    return;
                }

                // Checks if the streak mode config option is enabled
                if (module.isStreakModeEnabled()) {
                    // Resets RewardUser to Day 1 if a day has been missed
                    LocalDate lastCollectedDate = userData.getLastCollectedDate();
                    if (lastCollectedDate == null || (lastCollectedDate.isBefore(LocalDate.now().minusDays(1)) && !lastCollectedDate.isEqual(LocalDate.of(1971, 10, 1)))) {
                        userData.restartStreak();
                        LushRewards.getInstance().getDataManager().saveRewardUser(player);
                    }
                }

                // Checks if the reward has been collected today
                boolean collectedToday = userData.hasCollectedToday();
                // The current day number being shown to the user
                int currDayNum = userData.getDayNum();

                // First reward day shown
                AtomicInteger dayIndex = new AtomicInteger();
                final LocalDate[] dateIndex = new LocalDate[1];
                switch (module.getScrollType()) {
                    case GRID -> {
                        int rewardDisplaySize = guiTemplate.countChar('R');

                        int endDay = rewardDisplaySize;
                        while (currDayNum > endDay) {
                            endDay += rewardDisplaySize;
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

                TreeMultimap<Character, Integer> slotMap = TreeMultimap.create();
                for (int slot = 0; slot < inventory.getSize(); slot++) {
                    char character = guiTemplate.getCharAt(slot);
                    if (character == 'N') character = 'U';

                    slotMap.put(character, slot);
                }

                HashSet<String> collectedDates = userData.getCollectedDates();
                for (Character character : slotMap.keySet()) {
                    switch (character) {
                        case 'R' -> slotMap.get(character).forEach(slot -> {
                            ItemStack itemStack;
                            if (module.getScrollType().equals(ScrollType.MONTH) && dateIndex[0].getMonthValue() != LocalDate.now().getMonthValue()) {
                                SimpleItemStack simpleItemStack = LushRewards.getInstance().getConfigManager().getItemTemplate(String.valueOf('#'));

                                if (!simpleItemStack.hasType()) {
                                    simpleItemStack.setType(Material.STONE);
                                    LushRewards.getInstance().getLogger().severe("Failed to display custom item-template '#' as it does not specify a valid material");
                                }
                                simpleItemStack.parseColors(player);

                                itemStack = simpleItemStack.asItemStack(player);
                            } else {
                                // Get the day's reward for the current slot
                                RewardDay rewardDay = module.getRewardDay(dateIndex[0], dayIndex.get());
                                DailyRewardCollection priorityReward = rewardDay.getHighestPriorityRewardCollection();

                                String itemTemplate;
                                if (dayIndex.get() < currDayNum) {
                                    itemTemplate = (collectedDates.contains(dateIndex[0].format(DateTimeFormatter.ofPattern("dd-MM-yyyy")))) ? "collected-reward" : "missed-reward";
                                } else if (dayIndex.get() == currDayNum) {
                                    itemTemplate = collectedToday ? "collected-reward" : "redeemable-reward";
                                } else {
                                    itemTemplate = "default-reward";
                                }

                                SimpleItemStack displayItem;
                                if (!rewardDay.isEmpty()) {
                                    displayItem = SimpleItemStack.overwrite(LushRewards.getInstance().getConfigManager().getCategoryTemplate(priorityReward.getCategory()), LushRewards.getInstance().getConfigManager().getItemTemplate(itemTemplate), priorityReward.getDisplayItem());

                                    if (displayItem.getDisplayName() != null) {
                                        displayItem.setDisplayName(displayItem.getDisplayName()
                                            .replaceAll("%day%", String.valueOf(dayIndex.get()))
                                            .replaceAll("%month_day%", String.valueOf(dateIndex[0].getDayOfMonth()))
                                            .replaceAll("%month%", dateIndex[0].getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                                            .replaceAll("%month_num%", String.valueOf(dateIndex[0].getMonthValue()))
                                            .replaceAll("%year%", String.valueOf(dateIndex[0].getYear()))
                                            .replaceAll("%date%", dateIndex[0].format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                            .replaceAll("%date_us%", dateIndex[0].format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))));
                                    }

                                    if (displayItem.getLore() != null) {
                                        displayItem.setLore(displayItem.getLore().stream().map(line ->
                                            line.replaceAll("%day%", String.valueOf(dayIndex.get()))
                                                .replaceAll("%month_day%", String.valueOf(dateIndex[0].getDayOfMonth()))
                                                .replaceAll("%month%", dateIndex[0].getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                                                .replaceAll("%month_num%", String.valueOf(dateIndex[0].getMonthValue()))
                                                .replaceAll("%year%", String.valueOf(dateIndex[0].getYear()))
                                                .replaceAll("%date%", dateIndex[0].format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                                .replaceAll("%date_us%", dateIndex[0].format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
                                        ).toList());
                                    }

                                    displayItem.parseColors(player);
                                } else {
                                    displayItem = new SimpleItemStack(Material.AIR);
                                }

                                try {
                                    itemStack = displayItem.asItemStack(player);
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

                                        SimpleItemStack collectedItem = SimpleItemStack.overwrite(SimpleItemStack.from(currItem), LushRewards.getInstance().getConfigManager().getItemTemplate("collected-reward"));
                                        if (collectedItem.getDisplayName() != null) {
                                            collectedItem.setDisplayName(collectedItem.getDisplayName()
                                                .replaceAll("%day%", String.valueOf(currDayNum))
                                                .replaceAll("%month_day%", String.valueOf(dateIndex[0].getDayOfMonth()))
                                                .replaceAll("%month%", dateIndex[0].getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                                                .replaceAll("%month_num%", String.valueOf(dateIndex[0].getMonthValue()))
                                                .replaceAll("%year%", String.valueOf(dateIndex[0].getYear()))
                                                .replaceAll("%date%", dateIndex[0].format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                                .replaceAll("%date_us%", dateIndex[0].format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))));
                                        }
                                        collectedItem.parseColors(player);

                                        inventory.setItem(slot, collectedItem.asItemStack());

                                        Debugger.sendDebugMessage("Starting reward process for " + player.getName(), Debugger.DebugMode.ALL);

                                        module.claimRewards(player);

                                        LushRewards.getInstance().getEnabledRewardModules().forEach(module -> {
                                            if (module instanceof PlaytimeRewardsModule playtimeModule && playtimeModule.shouldReceiveWithDailyRewards()) {
                                                playtimeModule.claimRewards(player);
                                            }
                                        });
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
                                SimpleItemStack categoryItem = LushRewards.getInstance().getConfigManager().getCategoryTemplate(upcomingCategory);

                                // Get the day's reward for the current slot
                                SimpleItemStack simpleItemStack = SimpleItemStack.overwrite(categoryItem, LushRewards.getInstance().getConfigManager().getItemTemplate("upcoming-reward"), upcomingRewardCollection.getDisplayItem());

                                if (simpleItemStack.getDisplayName() != null) {
                                    simpleItemStack.setDisplayName(ChatColorHandler.translate(simpleItemStack
                                            .getDisplayName()
                                            .replaceAll("%day%", String.valueOf(upcomingRewardCollection.getRewardDayNum()))
                                            .replaceAll("%month_day%", String.valueOf(dateIndex[0].getDayOfMonth()))
                                            .replaceAll("%month%", dateIndex[0].getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                                            .replaceAll("%month_num%", String.valueOf(dateIndex[0].getMonthValue()))
                                            .replaceAll("%year%", String.valueOf(dateIndex[0].getYear()))
                                            .replaceAll("%date%", dateIndex[0].format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                            .replaceAll("%date_us%", dateIndex[0].format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))),
                                        player));
                                }

                                if (simpleItemStack.getLore() != null) {
                                    simpleItemStack.setLore(simpleItemStack.getLore().stream().map(line -> ChatColorHandler.translate(line, player)).toList());
                                }

                                ItemStack itemStack = simpleItemStack.asItemStack(player);
                                slotMap.get(character).forEach(slot -> inventory.setItem(slot, itemStack));
                            }
                        }
                        case ' ' ->
                            slotMap.get(character).forEach(slot -> inventory.setItem(slot, new ItemStack(Material.AIR)));
                        default -> slotMap.get(character).forEach(slot -> {
                            SimpleItemStack simpleItemStack = LushRewards.getInstance().getConfigManager().getItemTemplate(String.valueOf(character));

                            if (!simpleItemStack.hasType()) {
                                simpleItemStack.setType(Material.STONE);
                                LushRewards.getInstance().getLogger().severe("Failed to display custom item-template '" + character + "' as it does not specify a valid material");
                            }
                            simpleItemStack.parseColors(player);

                            inventory.setItem(slot, simpleItemStack.asItemStack(player));
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