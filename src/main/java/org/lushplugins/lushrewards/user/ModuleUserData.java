package org.lushplugins.lushrewards.user;

import org.lushplugins.lushlib.libraries.jackson.core.JsonProcessingException;
import org.lushplugins.lushrewards.LushRewards;

public abstract class ModuleUserData {

    public String asRawJson() {
        try {
            return LushRewards.BASIC_JSON_MAPPER.writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
