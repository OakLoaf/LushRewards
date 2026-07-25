package org.lushplugins.lushrewards.reward.module.dailyrewards;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lushplugins.guihandler.annotation.*;
import org.lushplugins.guihandler.gui.Gui;
import org.lushplugins.guihandler.gui.GuiAction;
import org.lushplugins.guihandler.gui.GuiActor;
import org.lushplugins.guihandler.slot.Slot;
import org.lushplugins.lushlib.libraries.chatcolor.ChatColorHandler;
import org.lushplugins.lushlib.utils.DisplayItemStack;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.reward.RewardDay;
import org.lushplugins.lushrewards.reward.module.playtimerewards.PlaytimeRewardsModule;
import org.lushplugins.lushrewards.user.RewardUser;
import org.lushplugins.lushrewards.utils.Debugger;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

@SuppressWarnings("unused")
@CustomGui
public record DailyRewardsGui(DailyRewardsModule module, ScrollType scrollType, boolean showDateAsAmount) {

    @GuiEvent(GuiAction.REFRESH)
    public void rewards(Gui gui, @Slots({'r', 'R'}) List<Slot> slots) {
        GuiActor actor = gui.actor();
        Player player = actor.player();
        UUID uuid = actor.uuid();
        RewardUser user = LushRewards.getInstance().getUserCache().getCachedUser(uuid);
        if (user == null) {
            return;
        }

        DailyRewardsUserData userData = user.getCachedModuleData(module.getId(), DailyRewardsUserData.class);
        module.checkRewardDay(uuid, userData);

        boolean collectedToday = userData.hasCollectedToday();
        int currDayNum = userData.getDayNum();

        // Collect first reward day and date shown
        GuiStartIndex startIndex = new GuiStartIndex(userData, slots.size());

        HashSet<Integer> collectedDays = userData.getCollectedDays();
        for (Slot slot : slots) {
            ItemStack itemStack;
            if (scrollType == ScrollType.MONTH && startIndex.date().getMonthValue() != LocalDate.now().getMonthValue()) {
                DisplayItemStack.Builder displayItemBuilder = DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getItemTemplate(String.valueOf('#'), module));

                if (!displayItemBuilder.hasType()) {
                    displayItemBuilder.setType(Material.STONE);
                }

                itemStack = displayItemBuilder.build().asItemStack(player, true);
            } else {
                // Get the day's reward for the current slot
                RewardDay rewardDay = module.getRewardDay(startIndex.date(), startIndex.day());
                DailyRewardCollection priorityReward = rewardDay.getHighestPriorityRewardCollection();

                String itemTemplate;
                if (startIndex.day() < currDayNum) {
                    itemTemplate = (collectedDays.contains(startIndex.day())) ? "collected-reward" : "missed-reward";
                } else if (startIndex.day() == currDayNum) {
                    itemTemplate = collectedToday ? "collected-reward" : "redeemable-reward";
                } else {
                    itemTemplate = "default-reward";
                }

                DisplayItemStack.Builder displayItemBuilder;
                if (!rewardDay.isEmpty()) {
                    displayItemBuilder = DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getCategoryTemplate(priorityReward.getCategory())).overwrite(
                        DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getItemTemplate(itemTemplate, module)),
                        DisplayItemStack.builder(priorityReward.getDisplayItem()));

                    if (displayItemBuilder.getDisplayName() != null) {
                        displayItemBuilder.setDisplayName(displayItemBuilder.getDisplayName()
                            .replace("%claimed%", itemTemplate.equals("collected-reward") ? module.getRewardPlaceholderClaimed() : module.getRewardPlaceholderUnclaimed())
                            .replace("%day%", String.valueOf(startIndex.day()))
                            .replace("%month_day%", String.valueOf(startIndex.date().getDayOfMonth()))
                            .replace("%month%", startIndex.date().getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                            .replace("%month_num%", String.valueOf(startIndex.date().getMonthValue()))
                            .replace("%year%", String.valueOf(startIndex.date().getYear()))
                            .replace("%date%", startIndex.date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                            .replace("%date_us%", startIndex.date().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))));
                    }

                    if (displayItemBuilder.getLore() != null) {
                        displayItemBuilder.setLore(displayItemBuilder.getLore().stream().map(line ->
                            line.replace("%claimed%", itemTemplate.equals("collected-reward") ? module.getRewardPlaceholderClaimed() : module.getRewardPlaceholderUnclaimed())
                                .replace("%day%", String.valueOf(startIndex.dayIndex))
                                .replace("%month_day%", String.valueOf(startIndex.date().getDayOfMonth()))
                                .replace("%month%", startIndex.date().getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                                .replace("%month_num%", String.valueOf(startIndex.date().getMonthValue()))
                                .replace("%year%", String.valueOf(startIndex.date().getYear()))
                                .replace("%date%", startIndex.date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                .replace("%date_us%", startIndex.date().format(DateTimeFormatter.ofPattern("MM/dd/yyyy")))
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
                if (startIndex.day() == currDayNum && !collectedToday) {
                    slot.button((event, context) -> {
                        // Gets clicked item and checks if it exists
                        ItemStack currItem = event.getCurrentItem();
                        if (currItem == null) {
                            return;
                        }

                        Debugger.sendDebugMessage("Starting reward process for " + player.getName(), Debugger.DebugMode.ALL);
                        module.claimRewards(player, user);

                        LushRewards.getInstance().getRewardModuleManager().getModules(PlaytimeRewardsModule.class).forEach(module -> {
                            if (module.shouldReceiveWithDailyRewards()) {
                                module.claimRewards(player, user);
                            }
                        });

                        DisplayItemStack.Builder collectedItemBuilder = DisplayItemStack.builder(currItem).overwrite(
                            DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getItemTemplate("collected-reward", module)));

                        if (collectedItemBuilder.getDisplayName() != null) {
                            collectedItemBuilder.setDisplayName(collectedItemBuilder.getDisplayName()
                                .replace("%claimed%", itemTemplate.equals("collected-reward") ? module.getRewardPlaceholderClaimed() : module.getRewardPlaceholderUnclaimed())
                                .replace("%day%", String.valueOf(currDayNum))
                                .replace("%month_day%", String.valueOf(startIndex.date().getDayOfMonth()))
                                .replace("%month%", startIndex.date().getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                                .replace("%month_num%", String.valueOf(startIndex.date().getMonthValue()))
                                .replace("%year%", String.valueOf(startIndex.date().getYear()))
                                .replace("%date%", startIndex.date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                .replace("%date_us%", startIndex.date().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))));
                        }

                        slot.icon(collectedItemBuilder.build().asItemStack(player, true));
                        gui.refresh(slot);
                    });
                }

                // Sets the size of the stack to the same amount as the current date
                if (showDateAsAmount) {
                    itemStack.setAmount(startIndex.date().getDayOfMonth());
                }
            }

            slot.icon(itemStack);

            startIndex.incrementDay(1);
            startIndex.incrementDate(1);
        }
    }

    @IconProvider({'u', 'n', 'U', 'N'})
    public ItemStack upcomingRewardIcon(GuiActor actor, @Slots({'r', 'R'}) List<Slot> rewardSlots) {
        Player player = actor.player();
        UUID uuid = actor.uuid();
        RewardUser user = LushRewards.getInstance().getUserCache().getCachedUser(uuid);
        if (user == null) {
            return null;
        }

        DailyRewardsUserData userData = user.getCachedModuleData(module.getId(), DailyRewardsUserData.class);
        GuiStartIndex startIndex = new GuiStartIndex(userData, rewardSlots.size());

        String upcomingCategory = module.getUpcomingCategory();
        Optional<DailyRewardCollection> upcomingReward = module.findNextRewardFromCategory(startIndex.day(), startIndex.date(), upcomingCategory);

        // Adds the upcoming reward to the GUI if it exists
        if (!upcomingReward.isPresent()) {
            return null;
        }

        DailyRewardCollection upcomingRewardCollection = upcomingReward.get();

        // Get the day's reward for the current slot
        DisplayItemStack.Builder categoryItemBuilder = DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getCategoryTemplate(upcomingCategory)).overwrite(
            DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getItemTemplate("upcoming-reward", module)),
            DisplayItemStack.builder(upcomingRewardCollection.getDisplayItem()));

        if (categoryItemBuilder.getDisplayName() != null) {
            categoryItemBuilder.setDisplayName(ChatColorHandler.translate(categoryItemBuilder
                    .getDisplayName()
                    .replace("%day%", String.valueOf(upcomingRewardCollection.getRewardDayNum()))
                    .replace("%month_day%", String.valueOf(startIndex.date().getDayOfMonth()))
                    .replace("%month%", startIndex.date().getMonth().getDisplayName(TextStyle.FULL, Locale.US))
                    .replace("%month_num%", String.valueOf(startIndex.date().getMonthValue()))
                    .replace("%year%", String.valueOf(startIndex.date().getYear()))
                    .replace("%date%", startIndex.date().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                    .replace("%date_us%", startIndex.date().format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))),
                player));
        }

        if (categoryItemBuilder.getLore() != null) {
            categoryItemBuilder.setLore(categoryItemBuilder.getLore().stream().map(line -> ChatColorHandler.translate(line, player)).toList());
        }

        return categoryItemBuilder.build().asItemStack(actor.player());
    }

    private class GuiStartIndex {
        private int dayIndex;
        private LocalDate dateIndex;

        public GuiStartIndex(int dayIndex, LocalDate dateIndex) {
            this.dayIndex = dayIndex;
            this.dateIndex = dateIndex;
        }

        public GuiStartIndex(DailyRewardsUserData userData, int rewardDisplaySize) {
            int currDayNum = userData.getDayNum();

            switch (scrollType) {
                case GRID -> {
                    int endDay = rewardDisplaySize;
                    if (rewardDisplaySize >= 1) {
                        while (currDayNum > endDay) {
                            endDay += rewardDisplaySize;
                        }
                    }

                    this.dayIndex = endDay - (rewardDisplaySize - 1);

                    int diff = this.dayIndex - currDayNum;
                    this.dateIndex = LocalDate.now().plusDays(diff);
                }
                case MONTH -> {
                    LocalDate today = LocalDate.now();
                    this.dayIndex = currDayNum - (today.getDayOfMonth() - 1);
                    this.dateIndex = LocalDate.of(today.getYear(), today.getMonth(), 1);
                }
                default -> {
                    this.dayIndex = currDayNum;
                    this.dateIndex = LocalDate.now();
                }
            }
        }

        public int day() {
            return dayIndex;
        }

        public void day(int day) {
            this.dayIndex = day;
        }

        public void incrementDay(int increment) {
            this.dayIndex += increment;
        }

        public LocalDate date() {
            return dateIndex;
        }

        public void date(LocalDate date) {
            this.dateIndex = date;
        }

        public void incrementDate(int daysIncrement) {
            this.dateIndex = dateIndex.plusDays(daysIncrement);
        }
    }

    public enum ScrollType {
        DAY,
        MONTH,
        GRID
    }
}
