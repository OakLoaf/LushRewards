package org.lushplugins.lushrewards.data;

import com.mysql.cj.jdbc.MysqlDataSource;
import org.lushplugins.lushrewards.LushRewards;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MySqlStorage extends AbstractStorage {
    private static final Logger log = LushRewards.getInstance().getLogger();

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

        String query = String.format("SELECT %s FROM %s", column, table);
        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(query)
        ) {
            stmt.executeQuery();
        } catch (SQLException assertException) {
            if (assertException.getErrorCode() == 1054) { // Undefined column error code in MySQL
                String statement = MessageFormat.format("ALTER TABLE {0} ADD COLUMN {1} {2};", table, column, type);
                try (Connection conn = conn();
                     PreparedStatement stmt = conn.prepareStatement(statement)
                ) {
                    stmt.execute();
                } catch (SQLException alterException) {
                    log.log(Level.SEVERE, "Error while asserting column", alterException);
                }
            } else {
                log.log(Level.SEVERE, "Error while asserting column", assertException);
            }
        }
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
