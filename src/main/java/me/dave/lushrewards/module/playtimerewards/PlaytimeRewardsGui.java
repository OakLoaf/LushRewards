package me.dave.lushrewards.module.playtimerewards;

import com.google.common.collect.TreeMultimap;
import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.gui.GuiFormat;
import me.dave.platyutils.gui.inventory.Gui;
import me.dave.platyutils.libraries.chatcolor.ChatColorHandler;
import me.dave.platyutils.utils.SimpleItemStack;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PlaytimeRewardsGui extends Gui {
    private final PlaytimeRewardsModule module;
    private final GuiFormat.GuiTemplate guiTemplate;

    public PlaytimeRewardsGui(PlaytimeRewardsModule module, Player player) {
        super(module.getGuiFormat().getTemplate().getRowCount() * 9, ChatColorHandler.translate(module.getGuiFormat().getTitle()), player);
        this.module = module;
        this.guiTemplate = module.getGuiFormat().getTemplate();
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

                TreeMultimap<Character, Integer> slotMap = guiTemplate.getSlotMap();
                for (Character character : slotMap.keySet()) {
                    switch (character) {
                        case 'A' -> slotMap.get(character).forEach(slot -> {
                            // TODO: Replace with category
                            SimpleItemStack simpleItemStack = new SimpleItemStack(Material.STONE);
                            if (module.hasClaimableRewards(player)) {
                                simpleItemStack.setEnchanted(true);
                            }
                            simpleItemStack.parseColors(player);

                            inventory.setItem(slot, simpleItemStack.asItemStack(player));

                            addButton(slot, event -> {
                                removeButton(slot);

                                if (module.hasClaimableRewards(player)) {
                                    module.claimRewards(player);
                                }
                            });
                        });
                        case 'R' -> {}
                        case ' ' -> slotMap.get(character).forEach(slot -> inventory.setItem(slot, new ItemStack(Material.AIR)));
                        default -> slotMap.get(character).forEach(slot -> {
                            SimpleItemStack simpleItemStack = LushRewards.getInstance().getConfigManager().getItemTemplate(String.valueOf(character));

                            if (!simpleItemStack.hasType()) {
                                simpleItemStack.setType(Material.RED_STAINED_GLASS_PANE);
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
}
