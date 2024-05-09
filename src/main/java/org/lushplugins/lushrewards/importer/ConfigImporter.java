package org.lushplugins.lushrewards.importer;

import org.lushplugins.lushrewards.LushRewards;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushlib.utils.FilenameUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class ConfigImporter {
    private final String pluginName;
    private final File dataFolder;

    public ConfigImporter(String pluginName) throws FileNotFoundException {
        this.pluginName = pluginName;
        this.dataFolder = new File(LushRewards.getInstance().getDataFolder().getParentFile(), pluginName);

        if (!dataFolder.exists()) {
            throw new FileNotFoundException();
        }
    }

    public String getPluginName() {
        return pluginName;
    }

    public File getDataFolder() {
        return dataFolder;
    }

    public abstract boolean startImport();

    @Nullable
    protected static File prepareForImport(File oldFile) {
        return prepareForImport(oldFile, true);
    }

    @Nullable
    protected static File prepareForImport(File oldFile, boolean createNew) {
        File parent = oldFile.getParentFile();
        String name = oldFile.getName();

        if (!oldFile.renameTo(new File(parent, FilenameUtils.removeExtension(name) + "-old-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy--HH-mm-ss")) + ".yml"))) {
            LushRewards.getInstance().getLogger().severe("Failed to rename file");
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
