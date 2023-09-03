package me.dave.activityrewarder.gui;

import me.dave.activityrewarder.gui.abstracts.AbstractGui;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InventoryHandler {
    private static final ConcurrentHashMap<UUID, AbstractGui> playerInventoryMap = new ConcurrentHashMap<>();

    public static AbstractGui getGui(UUID uuid) {
        return playerInventoryMap.get(uuid);
    }

    public static void putInventory(UUID uuid, AbstractGui gui) {
        playerInventoryMap.put(uuid, gui);
    }

    public static void removeInventory(UUID uuid) {
        playerInventoryMap.remove(uuid);
    }

    public static void closeAll() {
        playerInventoryMap.keySet().forEach(uuid -> {
            Player player = Bukkit.getPlayer(uuid);

            if (player != null) {
                player.closeInventory();
            }
        });
    }
}
