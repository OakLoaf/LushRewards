package org.lushplugins.lushrewards.data;

import org.lushplugins.lushrewards.LushRewards;
import org.postgresql.ds.PGSimpleDataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;


public class PostgreSqlStorage extends AbstractStorage {
    private static final Logger log = LushRewards.getInstance().getLogger();

    public PostgreSqlStorage(String host, int port, String databaseName, String user, String password, String schema) {
        super(initDataSource(host, port, databaseName, user, password, schema));
    }

    @Override
    protected String getUpsertStatement(String table, String column) {
        column = kebabCaseToSnakeCase(column);
        return MessageFormat.format(
                "INSERT INTO {0}(uuid, {1}) VALUES(?, ?) ON CONFLICT (uuid) DO UPDATE SET {1} = EXCLUDED.{1};",
                table, column
        );
    }

    @Override
    protected void assertJsonColumn(String table, String column) {
        assertColumn(table, column, "JSONB");
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
            if ("42703".equals(assertException.getSQLState())) { // Undefined column error code in PostgreSQL
                String statement = MessageFormat.format("ALTER TABLE {0} ADD COLUMN {1} {2};", table, column, type);
                try (Connection conn = conn();
                     PreparedStatement stmt = conn.prepareStatement(statement)
                ) {
                    stmt.execute();
                } catch (SQLException alterException) {
                    log.log(Level.SEVERE, "Error while alter column", alterException);
                }
            } else {
                log.log(Level.SEVERE, "Error while asserting column", assertException);
            }
        }
    }

    private static PGSimpleDataSource initDataSource(String host, int port, String dbName, String user, String password, String schema) {
        PGSimpleDataSource dataSource = new PGSimpleDataSource();
        dataSource.setServerNames(new String[]{host});
        dataSource.setPortNumbers(new int[]{port});
        dataSource.setDatabaseName(dbName);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        dataSource.setCurrentSchema(schema);
        return dataSource;
    }
}
