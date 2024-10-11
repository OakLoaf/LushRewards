package org.lushplugins.lushrewards.storage.migrator;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.lushplugins.lushlib.utils.FilenameUtils;
import org.lushplugins.lushrewards.LushRewards;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class Version3DataMigrator extends Migrator {
    private final File version2DataFolder;
    private final File lushDataFolder;

    public Version3DataMigrator() throws FileNotFoundException {
        super("Version3Migrator");
        version2DataFolder = new File(LushRewards.getInstance().getDataFolder().getParentFile(), "ActivityRewarder/data");
        lushDataFolder = new File(LushRewards.getInstance().getDataFolder(), "data");
    }

    @Override
    public boolean convert() {
        if (!version2DataFolder.exists()) {
            return false;
        }

        if (!lushDataFolder.exists()) {
            lushDataFolder.mkdirs();
        }

        AtomicInteger count = new AtomicInteger(0);
        long total = version2DataFolder.list().length;
        try (Stream<Path> stream = Files.list(version2DataFolder.toPath())) {
            stream
                .filter(file -> !Files.isDirectory(file))
                .parallel().forEach(entry -> {
                    File file = entry.toFile();
                    String uuid = FilenameUtils.removeExtension(file.getName());

                    try {
                        updateFile(file);
                        LushRewards.getInstance().getLogger().info("Translated user data for '" + uuid + "' (" + count.incrementAndGet() + "/" + total + ")");
                    } catch (IOException e) {
                        LushRewards.getInstance().getLogger().severe("Failed to translate user data for '" + uuid + "' (" + count.incrementAndGet() + "/" + total + ")");
                    }
                });
        } catch (IOException e) {
            LushRewards.getInstance().getLogger().severe("Failed to load data folder");
            return false;
        }

        return true;
    }

    public void updateFile(File file) throws IOException {
        String uuid = FilenameUtils.removeExtension(file.getName());
        YamlConfiguration data = YamlConfiguration.loadConfiguration(file);

        JsonObject json = new JsonObject();

        JsonObject main = new JsonObject();
        main.addProperty("username", data.getString("name"));
        main.addProperty("minutesPlayed", data.getInt("minutes-played"));
        json.add("main", main);

        LocalDate startDate = null;
        String startDateFormatted = null;
        ConfigurationSection dailyRewardsData = data.getConfigurationSection("daily-rewards");
        if (dailyRewardsData != null) {
            JsonObject module = new JsonObject();
            if (dailyRewardsData.contains("start-date")) {
                startDate = LocalDate.parse(dailyRewardsData.getString("start-date"), DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                startDateFormatted = startDate.format(DateTimeFormatter.ISO_LOCAL_DATE);
                module.addProperty("startDate", startDateFormatted);
            }
            module.addProperty("lastJoinDate", LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE));
            if (dailyRewardsData.contains("last-collected-date")) {
                module.addProperty("lastCollectedDate", LocalDate.parse(dailyRewardsData.getString("last-collected-date"), DateTimeFormatter.ofPattern("dd-MM-yyyy")).format(DateTimeFormatter.ISO_LOCAL_DATE));
            }

            JsonArray collectedDays = new JsonArray();
            if (startDate != null) {
                for (String dateRaw : dailyRewardsData.getStringList("collected-dates")) {
                    LocalDate date = LocalDate.parse(dateRaw, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                    long days = startDate.until(date).get(ChronoUnit.DAYS) + 1;
                    collectedDays.add(days);
                }
            }
            module.add("collectedDays", collectedDays);

            module.addProperty("dayNum", dailyRewardsData.getInt("day-num"));
            module.addProperty("streak", dailyRewardsData.getInt("streak"));
            module.addProperty("highestStreak", dailyRewardsData.getInt("highest-streak"));

            json.add("daily-rewards", module);
        }

        ConfigurationSection dailyPlaytimeData = data.getConfigurationSection("daily-playtime-goals");
        if (dailyPlaytimeData != null) {
            JsonObject module = new JsonObject();
            module.addProperty("lastCollectedPlaytime", dailyPlaytimeData.getString("last-collected-playtime"));
            module.addProperty("startDate", startDateFormatted);
            module.addProperty("previousDayEndPlaytime", 0);

            json.add("daily-playtime-rewards", module);
        }

        ConfigurationSection globalPlaytimeData = data.getConfigurationSection("global-playtime-goals");
        if (globalPlaytimeData != null) {
            JsonObject module = new JsonObject();
            module.addProperty("lastCollectedPlaytime", globalPlaytimeData.getString("last-collected-playtime"));
            module.addProperty("startDate", startDateFormatted);
            module.addProperty("previousDayEndPlaytime", 0);

            json.add("global-playtime-rewards", module);
        }

        FileWriter writer = new FileWriter(new File(lushDataFolder, uuid + ".json"));
        LushRewards.getInstance().getGson().toJson(json, writer);
        writer.flush();
    }
}
