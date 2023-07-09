package me.dave.activityrewarder.events;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.gui.InventoryHandler;
import me.dave.activityrewarder.gui.abstracts.AbstractGui;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class GuiEvents implements Listener {

    @EventHandler
    public void onItemClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();

        AbstractGui gui = InventoryHandler.getGui(playerUUID);
        if (gui == null) return;

        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null || !clickedInventory.equals(gui.getInventory())) return;

        gui.onClick(event);
    }

    @EventHandler
    public void onItemDrag(InventoryDragEvent event) {
        Player player = (Player) event.getWhoClicked();
        UUID playerUUID = player.getUniqueId();

        AbstractGui gui = InventoryHandler.getGui(playerUUID);
        if (gui == null) return;

        Inventory clickedInventory = event.getInventory();
        if (!clickedInventory.equals(gui.getInventory())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        UUID playerUUID = event.getPlayer().getUniqueId();

        AbstractGui gui = InventoryHandler.getGui(playerUUID);
        if (gui == null || !event.getInventory().equals(gui.getInventory())) return;

        InventoryHandler.removeInventory(playerUUID);
    }
}
