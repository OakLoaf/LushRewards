package me.dave.activityrewarder.importer.internal;

import me.dave.activityrewarder.importer.ConfigImporter;

import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;

public class ActivityRewarderModuleUpdater extends ConfigImporter {

    public ActivityRewarderModuleUpdater() throws FileNotFoundException {
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
