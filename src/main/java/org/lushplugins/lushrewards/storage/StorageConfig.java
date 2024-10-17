package org.lushplugins.lushrewards.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.lushplugins.lushrewards.LushRewards;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Level;

public abstract class StorageConfig {
    private final String storageType;

    public StorageConfig(ConfigurationSection config) {
        String storageType = config.getString("type");
        if (storageType == null) {
            LushRewards.getInstance().getLogger().warning("No storage type defined defaulted to sqlite");
            storageType = "sqlite";
        }

        this.storageType = storageType;
    }

    public String getStorageType() {
        return storageType;
    }

    protected abstract DataSource setupDataSource();

    protected void testDataSource(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1000)) {
                throw new SQLException("Could not establish database connection.");
            }
        } catch (SQLException e) {
            LushRewards.getInstance().log(Level.SEVERE, "An error occurred while testing the data source ", e);
        }
    }
}
