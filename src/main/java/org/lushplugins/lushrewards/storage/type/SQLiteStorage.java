package org.lushplugins.lushrewards.storage.type;

import org.bukkit.configuration.ConfigurationSection;
import org.lushplugins.lushrewards.LushRewards;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.logging.Level;

public class SQLiteStorage extends MySQLStorage {
    private static final String DATABASE_PATH = new File(LushRewards.getInstance().getDataFolder(), "data.db").getAbsolutePath();

    @Override
    protected String getInsertOrUpdateStatement(String table, String column) {
        return MessageFormat.format(
            "INSERT INTO `{0}`(uuid, `{1}`) VALUES(?, ?) ON CONFLICT (uuid) DO UPDATE SET `{1}` = EXCLUDED.`{1}`;",
            table, column
        );
    }

    @Override
    protected Connection conn() {
        try {
            return DriverManager.getConnection("jdbc:sqlite:" + DATABASE_PATH);
        } catch (SQLException e) {
            LushRewards.getInstance().log(Level.SEVERE, "An error occurred whilst getting a connection: ", e);
            return null;
        }
    }

    @Override
    protected void assertColumn(String table, String column, String type) {
        assertTable(table);

        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(String.format("SELECT `%s` FROM `%s`", column, table))
        ) {
            stmt.executeQuery();
        } catch (SQLException assertException) {
            if (assertException.getMessage().contains("no such column")) { // Undefined column error code
                try (Connection conn = conn(); PreparedStatement stmt = conn.prepareStatement(
                    String.format("ALTER TABLE `%s` ADD COLUMN `%s` %s;", table, column, type)
                )) {
                    stmt.execute();
                } catch (SQLException alterException) {
                    LushRewards.getInstance().getLogger().log(Level.SEVERE, "Failed to create column: ", alterException);
                }
            } else {
                LushRewards.getInstance().getLogger().log(Level.SEVERE, "Failed to assert column: ", assertException);
            }
        }
    }

    @Override
    protected DataSource setupDataSource(ConfigurationSection config) {
        return null;
    }
}
