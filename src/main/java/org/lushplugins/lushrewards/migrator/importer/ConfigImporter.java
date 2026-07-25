package org.lushplugins.lushrewards.migrator.importer;

import org.lushplugins.lushrewards.LushRewards;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushlib.utils.FilenameUtils;
import org.lushplugins.lushrewards.migrator.Migrator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public abstract class ConfigImporter extends Migrator {
    private final File dataFolder;

    public ConfigImporter(String pluginName) throws FileNotFoundException {
        super(pluginName);
        this.dataFolder = new File(LushRewards.getInstance().getDataFolder().getParentFile(), pluginName);

        if (!dataFolder.exists()) {
            throw new FileNotFoundException();
        }
    }

    public ConfigImporter(String name, File dataFolder) throws FileNotFoundException {
        super(name);
        this.dataFolder = dataFolder;

        if (!dataFolder.exists()) {
            throw new FileNotFoundException();
        }
    }

    public File getDataFolder() {
        return dataFolder;
    }

    @Override
    public boolean convert() {
        return startImport();
    }

    public abstract boolean startImport();

    @Nullable
    protected static File prepareForImport(File oldFile) {
        return prepareForImport(oldFile, true);
    }

    @Nullable
    protected static File prepareForImport(File oldFile, boolean createNew) {
        File parent = oldFile.getParentFile();
        File backupsDir = new File(parent, "backups");
        String name = oldFile.getName();

        if (!backupsDir.exists()) {
            backupsDir.mkdir();
        }

        if (!oldFile.renameTo(new File(backupsDir, FilenameUtils.removeExtension(name) + "-old-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy--HH-mm-ss")) + ".yml"))) {
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
