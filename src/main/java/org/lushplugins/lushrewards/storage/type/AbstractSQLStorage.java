package org.lushplugins.lushrewards.storage.type;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.configuration.ConfigurationSection;
import org.lushplugins.lushrewards.LushRewards;
import org.lushplugins.lushrewards.storage.Storage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    }

    @Override
    public JsonObject loadModuleUserDataJson(UUID uuid, String moduleId) {
        String table = moduleId != null ? USER_MODULES_TABLE : USER_TABLE;
        String column = formatHeader(moduleId != null ? moduleId + "_data" : "data");

        assertJsonColumn(table, column);

        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(String.format("SELECT `%s` FROM `%s` WHERE uuid = ?;", column, table))
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

    @Override
    public void saveModuleUserDataJson(UUID uuid, String moduleId, JsonObject json) {
        String table = moduleId != null ? USER_MODULES_TABLE : USER_TABLE;
        String column = formatHeader(moduleId != null ? moduleId + "_data" : "data");

        assertJsonColumn(table, column);

        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(getInsertOrUpdateStatement(table, column))
        ) {
            setUUIDToStatement(stmt, 1, uuid);
            setJsonToStatement(stmt, 2, json);
            stmt.executeUpdate();
        } catch (SQLException e) {
            LushRewards.getInstance().getLogger().log(Level.SEVERE, "Failed to save user data: ", e);
        }
    }

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

    protected abstract String getInsertOrUpdateStatement(String table, String column);

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
