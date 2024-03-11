package me.dave.lushrewards.importer.internal;

import me.dave.lushrewards.importer.ConfigImporter;

import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;

public class LushRewardsModuleUpdater extends ConfigImporter {

    public LushRewardsModuleUpdater() throws FileNotFoundException {
        super();
    }

    @Override
    protected String getPluginName() {
        return null;
    }

    @Override
    public CompletableFuture<Boolean> startImport() {
        return null;
    }
}
