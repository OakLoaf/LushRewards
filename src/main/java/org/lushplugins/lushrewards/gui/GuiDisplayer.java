package org.lushplugins.lushrewards.gui;

import org.bukkit.entity.Player;
import org.lushplugins.guihandler.gui.Gui;

public interface GuiDisplayer {

    Gui.Builder getGui();

    default void displayGui(Player player) {
        getGui().open(player);
    }
}
