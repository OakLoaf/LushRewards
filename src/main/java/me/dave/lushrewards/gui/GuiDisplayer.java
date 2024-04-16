package me.dave.lushrewards.gui;

import me.dave.platyutils.gui.inventory.Gui;
import org.bukkit.entity.Player;

public interface GuiDisplayer {

    Gui getGui(Player player);

    default void displayGui(Player player) {
        getGui(player).open();
    }
}
