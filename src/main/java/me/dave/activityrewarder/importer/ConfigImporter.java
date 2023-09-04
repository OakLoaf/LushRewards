package me.dave.activityrewarder.importer;

import me.dave.activityrewarder.ActivityRewarder;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;

public abstract class ConfigImporter {
    protected final File dataFolder;

    public ConfigImporter() throws FileNotFoundException {
        dataFolder = new File(ActivityRewarder.getInstance().getDataFolder().getParentFile(), getPluginName());

        if (!dataFolder.exists()) {
            throw new FileNotFoundException();
        }
    }

    protected abstract String getPluginName();

    public abstract CompletableFuture<Boolean> startImport();
}
