package org.lushplugins.lushrewards.storage.type;

import com.google.gson.JsonObject;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.lushplugins.lushrewards.LushRewards;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class MySQLStorage extends AbstractSQLStorage {

    @Override
    protected String getInsertOrUpdateStatement(String table, String column) {
        return String.format("REPLACE INTO `%s`(uuid, `%s`) VALUES(?, ?);", table, column);
    }

    @Override
    protected void setUUIDToStatement(PreparedStatement stmt, int index, UUID uuid) throws SQLException {
        stmt.setString(index, uuid.toString());
    }

    @Override
    protected void setJsonToStatement(PreparedStatement stmt, int index, JsonObject json) throws SQLException {
        stmt.setString(index, json.toString());
    }

    @SuppressWarnings("SameParameterValue")
    protected void assertColumn(String table, String column, String type) {
        assertTable(table);

        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(String.format("SELECT `%s` FROM `%s`", column, table))
        ) {
            stmt.executeQuery();
        } catch (SQLException assertException) {
            if (assertException.getErrorCode() == 1054) { // Undefined column error code
                try (Connection conn = conn(); PreparedStatement stmt = conn.prepareStatement(
                    String.format("ALTER TABLE `%s` ADD COLUMN `%s` %s;", table, column, type)
                )) {
                    stmt.execute();
                } catch (SQLException alterException) {
                    LushRewards.getInstance().getLogger().log(Level.SEVERE, "Failed to assert column: ", alterException);
                }
            } else {
                LushRewards.getInstance().getLogger().log(Level.SEVERE, "Failed to assert column: ", assertException);
            }
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
}
