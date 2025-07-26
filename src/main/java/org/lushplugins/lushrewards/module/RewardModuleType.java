package org.lushplugins.lushrewards.module;

import java.io.File;

public class RewardModuleType<T extends RewardModule> {
    private final String type;
    private final Constructor<T> constructor;

    public RewardModuleType(String type, Constructor<T> constructor) {
        this.type = type;
        this.constructor = constructor;
    }

    public String id() {
        return type;
    }

    public T build(String id, File file) {
        return this.constructor.build(id, file);
    }

    @FunctionalInterface
    public interface Constructor<T extends RewardModule> {
        T build(String id, File file);
    }
}
