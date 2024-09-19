package org.lushplugins.lushrewards.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerUtils {

    /**
     * Get a player's uuid from their username or UUID as a string
     * @param nameOrUuid A player's username or UUID as a string
     * @return The player's UUID
     * @throws IllegalArgumentException Thrown when a UUID cannot be found
     */
    public static UUID getUniqueId(String nameOrUuid) throws IllegalArgumentException {
        Player player = Bukkit.getPlayer(nameOrUuid);
        return player != null ? player.getUniqueId() : UUID.fromString(nameOrUuid);
    }
}
