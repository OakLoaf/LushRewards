package org.lushplugins.lushrewards.hook;

import org.geysermc.floodgate.api.FloodgateApi;
import org.lushplugins.lushlib.hook.Hook;

import java.util.UUID;

public class FloodgateHook extends Hook {

    public FloodgateHook() {
        super(HookId.FLOODGATE.toString());
    }

    public boolean isFloodgatePlayer(UUID uuid) {
        return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
    }
}
