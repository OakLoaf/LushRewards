package me.dave.activityrewarder.gui.abstracts;

import org.bukkit.event.inventory.InventoryClickEvent;

public abstract class AbstractGui {
    public abstract void recalculateContents();

    public abstract void openInventory();

    public abstract void onClick(InventoryClickEvent event);
}
