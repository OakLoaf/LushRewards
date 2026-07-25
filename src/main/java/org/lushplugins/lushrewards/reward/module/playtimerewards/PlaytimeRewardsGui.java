package org.lushplugins.lushrewards.reward.module.playtimerewards;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.lushplugins.guihandler.annotation.ButtonProvider;
import org.lushplugins.guihandler.annotation.CustomGui;
import org.lushplugins.guihandler.annotation.GuiEvent;
import org.lushplugins.guihandler.annotation.Slots;
import org.lushplugins.guihandler.gui.Gui;
import org.lushplugins.guihandler.gui.GuiAction;
import org.lushplugins.guihandler.gui.GuiActor;
import org.lushplugins.guihandler.slot.Slot;
import org.lushplugins.lushlib.utils.DisplayItemStack;
import org.lushplugins.lushlib.utils.Pair;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.playtime.PlaytimeTracker;
import org.lushplugins.lushrewards.user.RewardUser;
import org.lushplugins.lushrewards.utils.Debugger;
import org.lushplugins.lushrewards.utils.MathUtils;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;

@SuppressWarnings("unused")
@CustomGui
public record PlaytimeRewardsGui(PlaytimeRewardsModule module, ScrollType scrollType) {

    @GuiEvent(GuiAction.REFRESH)
    public void rewards(Gui gui, @Slots({'r', 'R'}) List<Slot> slots) {
        GuiActor actor = gui.actor();
        UUID uuid = actor.uuid();
        RewardUser user = LushRewards.getInstance().getUserCache().getCachedUser(uuid);
        if (user == null) {
            return;
        }

        PlaytimeRewardsUserData userData = user.getCachedModuleData(module.getId(), PlaytimeRewardsUserData.class);
        PlaytimeTracker playtimeTracker = LushRewards.getInstance().getPlaytimeTrackerManager().getPlaytimeTracker(uuid);

        int playtime = playtimeTracker.getGlobalPlaytime() - userData.getPreviousDayEndPlaytime();
        int lastCollectedPlaytime = userData.getLastCollectedPlaytime() - userData.getPreviousDayEndPlaytime();
        Integer shortestFrequency = module.getShortestRepeatFrequency(playtime);
        int startPlaytime;
        switch (scrollType) {
            case SCROLL -> {
                if (shortestFrequency != null) {
                    startPlaytime = Math.max((int) (playtime - (shortestFrequency * Math.floor(slots.size() / 2D))), lastCollectedPlaytime);
                } else {
                    startPlaytime = lastCollectedPlaytime;
                }
            }
            default -> startPlaytime = 0;
        }

        PriorityQueue<Pair<PlaytimeRewardCollection, Integer>> rewards = new PriorityQueue<>(Comparator.comparingInt(Pair::second));
        module.getRewards().forEach(reward -> {
            if (!reward.shouldHideFromGui()) {
                Integer minutes = MathUtils.findFirstNumInSequence(reward.getStartMinute(), reward.getRepeatFrequency(), startPlaytime);
                if (minutes != null) {
                    rewards.add(new Pair<>(reward, minutes));
                }
            }
        });

        Player player = gui.actor().player();
        for (Slot slot : slots) {
            if (rewards.isEmpty()) {
                return;
            }

            Pair<PlaytimeRewardCollection, Integer> rewardPair = rewards.poll();
            PlaytimeRewardCollection reward = rewardPair.first();
            int minutes = rewardPair.second();
            int nextMinute = minutes + reward.getRepeatFrequency();

            if (reward.getRepeatFrequency() > 0 && nextMinute <= reward.getRepeatsUntil()) {
                rewards.add(new Pair<>(reward, nextMinute));
            }

            String itemTemplate;
            if (minutes < lastCollectedPlaytime) {
                itemTemplate = "collected-reward";
            } else if (playtime > minutes) {
                itemTemplate = "redeemable-reward";
            } else {
                itemTemplate = "default-reward";
            }

            DisplayItemStack.Builder displayItemBuilder = DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getCategoryTemplate(reward.getCategory()))
                .overwrite(DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getItemTemplate(itemTemplate, module)))
                .overwrite(DisplayItemStack.builder(reward.getDisplayItem()));

            if (displayItemBuilder.getDisplayName() != null) {
                displayItemBuilder.setDisplayName(displayItemBuilder.getDisplayName()
                    .replace("%minutes%", String.valueOf(minutes)));
            }

            if (displayItemBuilder.hasLore()) {
                displayItemBuilder.setLore(displayItemBuilder.getLore().stream().map(line ->
                    line.replace("%minutes%", String.valueOf(minutes))
                ).toList());
            }

            if (module.hasClaimableRewardsAt(user, playtimeTracker.getGlobalPlaytime())) {
                slot.button((event, context) -> {
                    // Gets clicked item and checks if it exists
                    ItemStack currItem = event.getCurrentItem();
                    if (currItem == null) {
                        return;
                    }

                    DisplayItemStack.Builder collectedItemBuilder = DisplayItemStack.builder(currItem)
                        .overwrite(DisplayItemStack.builder(LushRewards.getInstance().getConfigManager().getItemTemplate("collected-reward", module)));

                    if (collectedItemBuilder.getDisplayName() != null) {
                        collectedItemBuilder.setDisplayName(collectedItemBuilder.getDisplayName()
                            .replace("%minutes%", String.valueOf(minutes)));
                    }

                    slot.icon(collectedItemBuilder.build().asItemStack(player, true));

                    Debugger.sendDebugMessage("Starting reward process for " + player.getName(), Debugger.DebugMode.ALL);
                    if (module.hasClaimableRewardsAt(user, playtimeTracker.getGlobalPlaytime())) {
                        module.claimRewards(player, user, playtimeTracker.getGlobalPlaytime());
                    }

                    gui.refresh();
                });
            }

            slot.icon(displayItemBuilder.build().asItemStack(player, true));
        }
    }

    @ButtonProvider({'a', 'A'})
    public void claimAll(GuiActor actor) {
        Player player = actor.player();
        RewardUser user = LushRewards.getInstance().getUserCache().getCachedUser(player.getUniqueId());
        if (module.hasClaimableRewards(player, user)) {
            module.claimRewards(player, user);
        }
    }

    public enum ScrollType {
        FIXED,
        SCROLL
    }
}
