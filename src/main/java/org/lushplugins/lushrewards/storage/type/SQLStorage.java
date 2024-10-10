package org.lushplugins.lushrewards.storage.type;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.data.RewardUser;
import org.lushplugins.lushrewards.module.UserDataModule;
import org.lushplugins.lushrewards.storage.Storage;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class SQLStorage extends Storage {

    @Override
    public void enable(ConfigurationSection config) {
        super.enable(config);
        setupDatabase("storage" + File.separator + "mysql_setup.sql");
    }

    @Override
    public RewardUser loadRewardUser(UUID uuid) {
        return null;
    }

    @Override
    public void saveRewardUser(RewardUser rewardUser) {

    }

    @Override
    public <T extends UserDataModule.UserData> T loadModuleUserData(UUID uuid, UserDataModule<T> module) {
        return null;
    }

    @Override
    public <T extends UserDataModule.UserData> void saveModuleUserData(UUID uuid, UserDataModule<T> module) {

    }

    protected void assertTable(String table) {
        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(String.format("CREATE TABLE IF NOT EXISTS %s (uuid UUID NOT NULL, PRIMARY KEY (uuid));", table))
        ) {
            stmt.execute();
        } catch (SQLException e) {
            LushRewards.getInstance().log(Level.SEVERE, "An error occurred while creating a table: ", e);
        }
    }

    protected void assertColumn(String table, String column, String type) {
        assertTable(table);

        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(String.format("SELECT %s FROM %s", column, table))
        ) {
            stmt.executeQuery();
        } catch (SQLException assertException) {
            if (assertException.getErrorCode() == 1054) { // Undefined column error code in MySQL
                try (Connection conn = conn();
                     PreparedStatement stmt = conn.prepareStatement(String.format("ALTER TABLE %s ADD COLUMN %s %s;", table, column, type))
                ) {
                    stmt.execute();
                } catch (SQLException alterException) {
                    LushRewards.getInstance().log(Level.SEVERE, "Error while asserting column: ", alterException);
                }
            } else {
                LushRewards.getInstance().log(Level.SEVERE, "Error while asserting column: ", assertException);
            }
        }
    }

    protected Connection conn() {
        try {
            return getDataSource().getConnection();
        } catch (SQLException e) {
            LushRewards.getInstance().log(Level.SEVERE, "An error occurred whilst getting a connection: ", e);
            return null;
        }
    }

    @Override
    protected DataSource setupDataSource(ConfigurationSection config) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setServerName(config.getString("host"));
        dataSource.setPortNumber(config.getInt("port"));
        dataSource.setDatabaseName(config.getString("database"));
        dataSource.setUser(config.getString("user"));
        dataSource.setPassword(config.getString("password"));

        return dataSource;
    }

    protected void setupDatabase(String fileName) {
        String setup;
        try (InputStream in = SQLStorage.class.getClassLoader().getResourceAsStream(fileName)) {
            setup = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining(""));
        } catch (IOException e) {
            LushRewards.getInstance().getLogger().log(Level.SEVERE, "Could not read db setup file: ", e);
            return;
        }

        String[] statements = setup.split("\\|");
        for (String statement : statements) {
            try (Connection conn = conn();
                 PreparedStatement stmt = conn.prepareStatement(statement))
            {
                stmt.execute();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        LushRewards.getInstance().getLogger().info("Database setup complete.");
    }
}
