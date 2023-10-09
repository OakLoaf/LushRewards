package me.dave.activityrewarder.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.dave.activityrewarder.ActivityRewarder;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Updater {
    private final ScheduledExecutorService updateExecutor = Executors.newScheduledThreadPool(1);
    private final Logger logger;

    private final String modrinthProjectSlug;
    private final String currentVersion;
    private final String jarName;
    private final String downloadCommand;

    private boolean enabled;
    private String latestVersion;
    private String downloadUrl;

    private boolean updateAvailable = false;
    private boolean ready = false;
    private boolean alreadyDownloaded = false;

    public Updater(Plugin plugin, String modrinthProjectSlug, String downloadCommand) {
        this.logger = plugin.getLogger();
        this.modrinthProjectSlug = modrinthProjectSlug;
        String currentVersion = plugin.getDescription().getVersion();
        this.currentVersion = currentVersion.contains("-") ? currentVersion.split("-")[0] : currentVersion;
        this.jarName = plugin.getDescription().getName();
        this.downloadCommand = downloadCommand;

        updateExecutor.scheduleAtFixedRate(() -> {
            try {
                check();
            } catch (Exception e) {
                logger.info("Unable to check for update: " + e.getMessage());
            }
        }, 2, 600, TimeUnit.SECONDS);
    }

    public void check() throws IOException {
        if (!enabled) {
            return;
        }

        URL url = new URL("https://api.modrinth.com/v2/project/" + modrinthProjectSlug + "/version?featured=true");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.addRequestProperty("User-Agent", jarName + "/" + currentVersion);

        if (connection.getResponseCode() != 200) {
            throw new IllegalStateException("Response code was " + connection.getResponseCode());
        }

        InputStream inputStream = connection.getInputStream();
        InputStreamReader reader = new InputStreamReader(inputStream);

        JsonArray versionsJson = JsonParser.parseReader(reader).getAsJsonArray();
        JsonObject currVersionJson = versionsJson.get(0).getAsJsonObject();

        latestVersion = currVersionJson.get("version_number").getAsString();
        downloadUrl = currVersionJson.get("files").getAsJsonArray().get(0).getAsJsonObject().get("url").getAsString();

        if (latestVersion.contains("-")) {
            latestVersion = latestVersion.split("-")[0];
        }

        if (latestVersion.isEmpty()) {
            throw new IllegalStateException("Latest version is empty!");
        }

        String[] parts = latestVersion.split("\\.");
        String[] currParts = currentVersion.split("\\.");

        int i = 0;
        for (String part : parts) {
            if (i >= currParts.length) {
                break;
            }

            int newVersion = Integer.parseInt(part);
            int currVersion = Integer.parseInt(currParts[i]);
            if (newVersion > currVersion) {
                if(i != 0) {
                    int newVersionLast = Integer.parseInt(parts[i-1]);
                    int currVersionLast = Integer.parseInt(currParts[i-1]);
                    if (newVersionLast >= currVersionLast) {
                        updateAvailable = true;
                        break;
                    }
                } else {
                    updateAvailable = true;
                    break;
                }
            }
            i++;
        }

        if (updateAvailable && !ready) {
            logger.info("An update is available! (" + latestVersion + ") Do /" + downloadCommand + " to download it!");
        } else if (!ready) {
            logger.info("You are up to date! (" + latestVersion + ")");
        }

        ready = true;
    }

    public boolean isUpdateAvailable() {
        return enabled && updateAvailable;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAlreadyDownloaded() {
        return alreadyDownloaded;
    }

    public boolean downloadUpdate() {
        if (!isEnabled()) {
            logger.warning("Updater is disabled");
            return false;
        }

        if (!isUpdateAvailable()) {
            logger.warning("No update is available!");
            return false;
        }

        if (isAlreadyDownloaded()) {
            logger.warning("The update has already been downloaded!");
            return false;
        }

        try {
            URL url = new URL(downloadUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.addRequestProperty("User-Agent", jarName + "/" + currentVersion);
            connection.setInstanceFollowRedirects(true);
            HttpURLConnection.setFollowRedirects(true);

            if (connection.getResponseCode() != 200) {
                throw new IllegalStateException("Response code was " + connection.getResponseCode());
            }

            ReadableByteChannel rbc = Channels.newChannel(connection.getInputStream());
            File out = new File(getUpdateFolder(), jarName + "-" + latestVersion + ".jar");
            logger.info(out.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(out);
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
            fos.close();

            updateAvailable = false;
            alreadyDownloaded = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void shutdown() {
        updateExecutor.shutdown();
    }

    private File getUpdateFolder() {
        File updateDir = new File(ActivityRewarder.getInstance().getDataFolder().getParentFile(), Bukkit.getUpdateFolder());

        if (!updateDir.exists()) {
            updateDir.mkdir();
        }

        return updateDir;
    }
}
