package org.lushplugins.lushrewards.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.data.RewardUser;
import org.lushplugins.lushrewards.module.UserDataModule;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public abstract class Storage {
    private DataSource dataSource;

    public void enable(ConfigurationSection config) {
        this.dataSource = setupDataSource(config);
        testDataSource(dataSource);
    }

    public void disable() {}

    public abstract RewardUser loadRewardUser(UUID uuid);

    public abstract void saveRewardUser(RewardUser rewardUser);

    public abstract <T extends UserDataModule.UserData> T loadModuleUserData(UUID uuid, UserDataModule<T> module);

    public abstract <T extends UserDataModule.UserData> void saveModuleUserData(UUID uuid, UserDataModule<T> module);

    protected DataSource getDataSource() {
        return dataSource;
    }

    protected abstract DataSource setupDataSource(ConfigurationSection config);

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
