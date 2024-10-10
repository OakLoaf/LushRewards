package org.lushplugins.lushrewards.olddata;

import com.google.gson.JsonObject;
import org.lushplugins.lushrewards.LushRewards;
import org.postgresql.ds.PGSimpleDataSource;
import org.postgresql.util.PGobject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.UUID;
import java.util.logging.Level;


public class PostgreSqlStorage extends AbstractSqlStorage {

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

        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(String.format("SELECT %s FROM %s", column, table))
        ) {
            stmt.executeQuery();
        } catch (SQLException assertException) {
            if ("42703".equals(assertException.getSQLState())) { // Undefined column error code in PostgreSQL
                try (Connection conn = conn();
                     PreparedStatement stmt = conn.prepareStatement(MessageFormat.format("ALTER TABLE {0} ADD COLUMN {1} {2};", table, column, type))
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
    protected void setUUIDToStatement(PreparedStatement stmt, int index, UUID uuid) throws SQLException {
        stmt.setObject(index, uuid);
    }

    @Override
    protected void setJsonToStatement(PreparedStatement stmt, int index, JsonObject jsonObject) throws SQLException {
        PGobject pgObject = new PGobject();
        pgObject.setType("jsonb");
        pgObject.setValue(jsonObject.toString());
        stmt.setObject(index, pgObject);
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
