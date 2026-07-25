package org.lushplugins.lushrewards.storage.type;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.storage.Storage;
import org.lushplugins.lushrewards.user.RewardUser;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public abstract class AbstractSQLStorage implements Storage {
    protected static final String USER_TABLE = "lushrewards_users";
    protected static final String USER_MODULES_TABLE = USER_TABLE + "_modules";

    private DataSource dataSource;

    @Override
    public void enable(ConfigurationSection config) {
        this.dataSource = setupDataSource(config);
        testDataSourceConnection();
        assertRewardUserTable();
    }

    @Override
    public @Nullable RewardUser prepareRewardUser(UUID uuid) {
        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(String.format("""
                 SELECT *
                 FROM %s
                 WHERE uuid = ?;
                 """, USER_TABLE))
        ) {
            setUUIDToStatement(stmt, 1, uuid);

            ResultSet results = stmt.executeQuery();
            return new RewardUser(
                uuid,
                results.getString("username"),
                results.getInt("minutesPlayed")
            );
        } catch (SQLException e) {
            LushRewards.getInstance().getLogger().log(Level.SEVERE, "Failed to load user data: ", e);
        }

        return null;
    }

    protected abstract String getInsertOrUpdateRewardUserStatement();

    @Override
    public void saveRewardUser(RewardUser user) {
        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(getInsertOrUpdateRewardUserStatement())
        ) {
            setUUIDToStatement(stmt, 1, user.getUniqueId());
            stmt.setString(2, user.getUsername());
            stmt.setInt(3, user.getMinutesPlayed());

            stmt.executeUpdate();
        } catch (SQLException e) {
            LushRewards.getInstance().getLogger().log(Level.SEVERE, "Failed to save user data: ", e);
        }
    }

    @Override
    public JsonObject loadModuleUserDataJson(UUID uuid, @NotNull String moduleId) {
        String column = formatHeader(moduleId + "_data");

        assertJsonColumn(USER_MODULES_TABLE, column);

        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(String.format("""
                 SELECT `%s`
                 FROM `%s`
                 WHERE uuid = ?;
                 """, column, USER_MODULES_TABLE))
        ) {
            setUUIDToStatement(stmt, 1, uuid);

            ResultSet resultSet = stmt.executeQuery();
            JsonObject json;
            if (resultSet.next()) {
                String jsonRaw = resultSet.getString(column);
                json = jsonRaw != null ? JsonParser.parseString(jsonRaw).getAsJsonObject() : null;
            } else {
                json = null;
            }

            return json;
        } catch (SQLException e) {
            LushRewards.getInstance().getLogger().log(Level.SEVERE, "Failed to load user data: ", e);
        }

        return null;
    }

    protected abstract String getInsertOrUpdateModuleUserDataStatement(String table, String column);

    @Override
    public void saveModuleUserDataJson(UUID uuid, String moduleId, JsonObject json) {
        String table = moduleId != null ? USER_MODULES_TABLE : USER_TABLE;
        String column = formatHeader(moduleId != null ? moduleId + "_data" : "data");

        assertJsonColumn(table, column);

        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(getInsertOrUpdateModuleUserDataStatement(table, column))
        ) {
            setUUIDToStatement(stmt, 1, uuid);
            setJsonToStatement(stmt, 2, json);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LushRewards.getInstance().getLogger().log(Level.SEVERE, "Failed to save user data: ", e);
        }
    }

    @Override
    public Collection<String> findSimilarUsernames(String input) {
        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(String.format("""
                 SELECT username
                 FROM %s
                 WHERE username LIKE CONCAT(?, '%%')
                 LIMIT 50;
                 """, USER_TABLE))
        ) {
            stmt.setString(1, input);

            List<String> usernames = new ArrayList<>();
            ResultSet results = stmt.executeQuery();
            while (results.next()) {
                usernames.add(results.getString("username"));
            }

            return usernames;
        } catch (SQLException e) {
            LushRewards.getInstance().getLogger().log(Level.SEVERE, "Failed to load user data: ", e);
        }

        return null;
    }

    protected void assertRewardUserTable() {
        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(String.format("""
                 CREATE TABLE IF NOT EXISTS %s (
                     uuid CHAR(36) NOT NULL,
                     username TEXT NOT NULL,
                     minutesPlayed INTEGER NOT NULL,
                     PRIMARY KEY (uuid)
                 );
                 """, USER_TABLE))
        ) {
            stmt.execute();
        } catch (SQLException e) {
            LushRewards.getInstance().getLogger().log(Level.SEVERE, "Failed to assert table: ", e);
        }
    }

    // TODO: Migrate assertion to run once on reload
    protected void assertTable(String table) {
        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(
                 String.format("CREATE TABLE IF NOT EXISTS `%s`(uuid CHAR(36) NOT NULL, PRIMARY KEY (uuid));", table))
        ) {
            stmt.execute();
        } catch (SQLException e) {
            LushRewards.getInstance().getLogger().log(Level.SEVERE, "Failed to assert table: ", e);
        }
    }

    protected abstract void setUUIDToStatement(PreparedStatement stmt, int index, UUID uuid) throws SQLException;

    protected abstract void setJsonToStatement(PreparedStatement stmt, int index, JsonObject json) throws SQLException;

    protected void assertJsonColumn(String table, String column) {
        assertColumn(table, column, "JSON");
    }

    protected abstract void assertColumn(String table, String column, String type);

    /**
     * Format column names
     */
    protected String formatHeader(String string) {
        return string;
    }

    protected Connection conn() {
        try {
            return getDataSource().getConnection();
        } catch (SQLException e) {
            LushRewards.getInstance().log(Level.SEVERE, "An error occurred whilst getting a connection: ", e);
            return null;
        }
    }

    protected DataSource getDataSource() {
        return dataSource;
    }

    protected abstract DataSource setupDataSource(ConfigurationSection config);

    protected void testDataSourceConnection() {
        try (Connection conn = conn()) {
            if (!conn.isValid(1000)) {
                throw new SQLException("Could not establish database connection.");
            }
        } catch (SQLException e) {
            LushRewards.getInstance().log(Level.SEVERE, "An error occurred while testing the data source ", e);
        }
    }
}
