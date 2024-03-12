package me.dave.lushrewards.hook;

import me.dave.platyutils.hook.Hook;
import org.geysermc.floodgate.api.FloodgateApi;

import java.util.UUID;

public class FloodgateHook extends Hook {

    public FloodgateHook() {
        super(HookId.FLOODGATE.toString());
    }

    public boolean isFloodgatePlayer(UUID uuid) {
        return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
    }
}
