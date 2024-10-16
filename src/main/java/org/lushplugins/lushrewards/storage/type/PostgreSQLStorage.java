package org.lushplugins.lushrewards.storage.type;

import com.google.gson.JsonObject;
import org.bukkit.configuration.ConfigurationSection;
import org.lushplugins.lushrewards.LushRewards;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.util.PGobject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;
import java.util.logging.Level;

public class PostgreSQLStorage extends AbstractSQLStorage {

    @Override
    protected void setUUIDToStatement(PreparedStatement stmt, int index, UUID uuid) throws SQLException {
        stmt.setObject(index, uuid);
    }

    @Override
    protected void setJsonToStatement(PreparedStatement stmt, int index, JsonObject json) throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        pgObject.setValue(json.toString());
        stmt.setObject(index, pgObject);
    }

    @Override
    protected void assertJsonColumn(String table, String column) {
        assertColumn(table, column, "JSONB");
    }

    @Override
    protected void assertColumn(String table, String column, String type) {
        assertTable(table);

        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(String.format("SELECT `%s` FROM `%s`", column, table))
        ) {
            stmt.executeQuery();
        } catch (SQLException assertException) {
            if (Objects.equals(assertException.getSQLState(), "42703")) { // Undefined column error code
                try (Connection conn = conn();
                     PreparedStatement stmt = conn.prepareStatement(String.format("ALTER TABLE `%s` ADD COLUMN `%s` %s;", table, column, type))
                ) {
                    stmt.execute();
                } catch (SQLException alterException) {
                    LushRewards.getInstance().log(Level.SEVERE, "Error while alter column", alterException);
                }
            } else {
                LushRewards.getInstance().log(Level.SEVERE, "Error while asserting column", assertException);
            }
        }
    }

    @Override
    protected String formatHeader(String string) {
        return string.replace("-", "_");
    }

    @Override
    protected DataSource setupDataSource(ConfigurationSection config) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setServerNames(new String[]{config.getString("host")});
        dataSource.setPortNumbers(new int[]{config.getInt("port")});
        dataSource.setDatabaseName(config.getString("database"));
        dataSource.setUser(config.getString("user"));
        dataSource.setPassword(config.getString("password"));
        dataSource.setCurrentSchema(config.getString("schema"));

        return dataSource;
    }
}
