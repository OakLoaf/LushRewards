package me.dave.activityrewarder.gui.abstracts;

import me.dave.activityrewarder.gui.InventoryHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractGui {
    protected final Inventory inventory;
    protected final Player player;
    protected final HashMap<Integer, Consumer<InventoryClickEvent> > buttons = new HashMap<>();
    private final HashMap<Integer, Boolean> slotLockMap = new HashMap<>();

    public AbstractGui(int size, String title, Player player) {
        inventory = Bukkit.createInventory(null, size, title);
        this.player = player;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public Player getPlayer() {
        return player;
    }

    protected void setItem(int slot, ItemStack item) {
        if (item == null) item = new ItemStack(Material.AIR);
        inventory.setItem(slot, item);
    }

    public void addButton(int slot, Consumer<InventoryClickEvent> task) {
        buttons.put(slot, task);
    }

    public void removeButton(int slot) {
        buttons.remove(slot);
    }

    public void clearButtons() {
        buttons.clear();
    }

    public void lockSlot(int slot) {
        lockSlot(slot, true);
    }

    public void unlockSlot(int slot) {
        lockSlot(slot, false);
    }

    public void lockSlot(int slot, boolean locked) {
        slotLockMap.put(slot, locked);
    }

    public void lockSlots(int... slots) {
        for (int slot : slots) {
            lockSlot(slot, true);
        }
    }

    public void unlockSlots(int... slots) {
        for (int slot : slots) {
            lockSlot(slot, false);
        }
    }

    public boolean isSlotLocked(int slot) {
        return slotLockMap.getOrDefault(slot, true);
    }

    public abstract void recalculateContents();

    public void openInventory() {
        recalculateContents();
        player.openInventory(inventory);
        InventoryHandler.putInventory(player.getUniqueId(), this);
    }


    // Event Methods
    public void onOpen(InventoryOpenEvent event) {}

    public void onClose(InventoryCloseEvent event) {
        InventoryHandler.removeInventory(player.getUniqueId());
    }

    public void onClick(InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) return;

        int slot = event.getRawSlot();

        Consumer<InventoryClickEvent> button = buttons.get(slot);
        if (button != null) {
            button.accept(event);
        }

        switch (event.getAction()) {
            case COLLECT_TO_CURSOR -> {
                event.setCancelled(true);
            }
            case DROP_ALL_SLOT, DROP_ONE_SLOT, PLACE_ALL, PLACE_SOME, PLACE_ONE, PICKUP_ALL, PICKUP_HALF, PICKUP_SOME, PICKUP_ONE, SWAP_WITH_CURSOR, CLONE_STACK  -> {
                if (clickedInventory.equals(inventory) && isSlotLocked(slot)) {
                    event.setCancelled(true);
                }
            }
            case MOVE_TO_OTHER_INVENTORY -> {
                event.setCancelled(true);
                if (!clickedInventory.equals(inventory)) {
                    List<Integer> unlockedSlots = slotLockMap.entrySet()
                            .stream()
                            .filter(entry -> !entry.getValue())
                            .map(Map.Entry::getKey)
                            .sorted()
                            .toList();

                    ItemStack clickedItem = event.getCurrentItem();
                    if (clickedItem != null) {
                        int remainingToDistribute = clickedItem.getAmount();
                        int backupDestinationSlot = -1;
                        boolean complete = false;
                        for (int unlockedSlot : unlockedSlots) {
                            if (complete) break;

                            ItemStack slotItem = inventory.getItem(unlockedSlot);
                            if ((slotItem == null || slotItem.getType() == Material.AIR) && backupDestinationSlot == -1) {
                                backupDestinationSlot = unlockedSlot;
                                continue;
                            }

                            if (slotItem != null && slotItem.isSimilar(clickedItem)) {
                                int spaceInStack = slotItem.getMaxStackSize() - slotItem.getAmount();

                                if (spaceInStack <= remainingToDistribute) {
                                    slotItem.setAmount(slotItem.getAmount() + remainingToDistribute);
                                    clickedItem.setType(Material.AIR);
                                    complete = true;
                                }
                                else if (spaceInStack > 0) {
                                    remainingToDistribute -= spaceInStack;
                                    slotItem.setAmount(slotItem.getMaxStackSize());
                                    clickedItem.setAmount(clickedItem.getAmount() - spaceInStack);
                                }
                            }
                        }

                        if (!complete && backupDestinationSlot != -1) {
                            inventory.setItem(backupDestinationSlot, clickedItem);
                            event.getInventory().setItem(event.getSlot(), null);
                        }
                    }
                }
            }
        }
    }

    public void onDrag(InventoryDragEvent event) {
        for (int slot : event.getRawSlots()) {
            if (slot <= 53 && isSlotLocked(slot)) {
                event.setCancelled(true);
                return;
            }
        }
    }
}