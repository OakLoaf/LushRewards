package me.dave.activityrewarder.importer;

import me.dave.activityrewarder.ActivityRewarder;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    @Nullable
    protected static File prepareForImport(File oldFile) {
        return prepareForImport(oldFile, true);
    }

    @Nullable
    protected static File prepareForImport(File oldFile, boolean createNew) {
        File parent = oldFile.getParentFile();
        String name = oldFile.getName();

        if (!oldFile.renameTo(new File(parent, FilenameUtils.removeExtension(name) + "-old-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy--HH-mm-ss")) + ".yml"))) {
            ActivityRewarder.getInstance().getLogger().severe("Failed to rename file");
            return null;
        }

        File newFile = new File(parent, name);

        if (createNew) {
            try {
                newFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return newFile;
    }
}
