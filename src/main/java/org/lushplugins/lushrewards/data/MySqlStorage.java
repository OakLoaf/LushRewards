package org.lushplugins.lushrewards.data;

import com.google.gson.JsonObject;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.lushplugins.lushrewards.LushRewards;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.UUID;
import java.util.logging.Level;


public class MySqlStorage extends AbstractSqlStorage {

    public MySqlStorage(String host, int port, String databaseName, String user, String password) {
        super(initDataSource(host, port, databaseName, user, password));
    }

    @Override
    protected String getUpsertStatement(String table, String column) {
        return MessageFormat.format(
                "REPLACE INTO {0}(uuid, {1}) VALUES(?, ?);",
                table, column
        );
    }

    @Override
    protected void assertJsonColumn(String table, String column) {
        assertColumn(table, column, "JSON");
    }

    @Override
    protected void assertColumn(String table, String column, String type) {
        assertTable(table);

        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(String.format("SELECT %s FROM %s", column, table))
        ) {
            stmt.executeQuery();
        } catch (SQLException assertException) {
            if (assertException.getErrorCode() == 1054) { // Undefined column error code in MySQL
                try (Connection conn = conn();
                     PreparedStatement stmt = conn.prepareStatement(MessageFormat.format("ALTER TABLE {0} ADD COLUMN {1} {2};", table, column, type))
                ) {
                    stmt.execute();
                } catch (SQLException alterException) {
                    LushRewards.getInstance().log(Level.SEVERE, "Error while asserting column", alterException);
                }
            } else {
                LushRewards.getInstance().log(Level.SEVERE, "Error while asserting column", assertException);
            }
        }
    }

    @Override
    protected void setUUIDToStatement(PreparedStatement stmt, int index, UUID uuid) throws SQLException {
        stmt.setString(index, uuid.toString());
    }

    @Override
    protected void setJsonToStatement(PreparedStatement stmt, int index, JsonObject jsonObject) throws SQLException {
        stmt.setString(index, jsonObject.toString());
    }

    private static MysqlDataSource initDataSource(String host, int port, String dbName, String user, String password) {
        MysqlDataSource dataSource = new MysqlDataSource();
        dataSource.setServerName(host);
        dataSource.setPortNumber(port);
        dataSource.setDatabaseName(dbName);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        return dataSource;
    }
}
