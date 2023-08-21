package me.dave.activityrewarder.gui;

import me.dave.activityrewarder.gui.abstracts.AbstractGui;

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
}
