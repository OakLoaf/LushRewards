package me.dave.lushrewards.importer;

import me.dave.platyutils.PlatyUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.CompletableFuture;

public class NDailyRewardsImporter extends ConfigImporter {

    public NDailyRewardsImporter() throws FileNotFoundException {
        super();
    }

    @Override
    protected String getPluginName() {
        return "NDailyRewards";
    }

    @Override
    public CompletableFuture<Boolean> startImport() {
        CompletableFuture<Boolean> completableFuture = new CompletableFuture<>();

        PlatyUtils.getMorePaperLib().scheduling().asyncScheduler().run(() -> {
            YamlConfiguration mainConfig = YamlConfiguration.loadConfiguration(new File(dataFolder, "config.yml"));

            completableFuture.complete(true);
        });

        return completableFuture;
    }
}
