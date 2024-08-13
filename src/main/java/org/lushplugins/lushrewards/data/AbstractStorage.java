package org.lushplugins.lushrewards.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.enchantedskies.EnchantedStorage.Storage;
import org.lushplugins.lushrewards.LushRewards;
import org.postgresql.util.PGobject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractStorage implements Storage<DataManager.StorageData, DataManager.StorageLocation> {
    protected static final String TABLE_NAME = "lushrewards_users";
    protected static final String MODULES_TABLE_NAME = "lushrewards_users_modules";
    private static final Logger log = LushRewards.getInstance().getLogger();

    protected final DataSource dataSource;

    protected AbstractStorage(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public DataManager.StorageData load(DataManager.StorageLocation storageLocation) {
        UUID uuid = storageLocation.uuid();
        String module = storageLocation.moduleId();

        String table;
        String column;
        if (module != null) {
            table = MODULES_TABLE_NAME;
            column = module + "_data";
        } else {
            table = TABLE_NAME;
            column = "data";
        }

        if (this instanceof PostgreSqlStorage) {
            column = kebabCaseToSnakeCase(column);
        }
        assertJsonColumn(table, column);

        String selectStatement = String.format("SELECT %s FROM %s WHERE uuid = ?;", column, table);
        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(selectStatement)
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

            return new DataManager.StorageData(uuid, module, json);
        } catch (SQLException e) {
            log.log(Level.SEVERE, "An error occurred while loading data for ", e);
        }

        return null;
    }

    @Override
    public void save(DataManager.StorageData storageData) {
        UUID uuid = storageData.uuid();
        String module = storageData.moduleId();
        JsonObject json = storageData.json();
        if (json == null) {
            throw new NullPointerException("JsonObject cannot be null when saving");
        }

        String table;
        String column;
        if (module != null) {
            table = MODULES_TABLE_NAME;
            column = module + "_data";
        } else {
            table = TABLE_NAME;
            column = "root_data";
        }

        if (this instanceof PostgreSqlStorage) {
            column = kebabCaseToSnakeCase(column);
        }

        assertJsonColumn(table, column);

        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(getUpsertStatement(table, column))
        ) {
            setUUIDToStatement(stmt, 1, uuid);
            setJsonToStatement(stmt, 2, json);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.log(Level.SEVERE, "An error occurred while saving data for ", e);
        }
    }

    protected abstract String getUpsertStatement(String table, String column);

    protected abstract void assertJsonColumn(String table, String column);

    protected abstract void assertColumn(String table, String column, String type);

    protected void assertTable(String table) {
        String statement = String.format("CREATE TABLE IF NOT EXISTS %s (uuid UUID NOT NULL, PRIMARY KEY (uuid));", table);
        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(statement)
        ) {
            stmt.execute();
        } catch (SQLException e) {
            log.log(Level.SEVERE, "An error occurred while creating table ", e);
        }
    }

    protected Connection conn() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            log.log(Level.SEVERE, "An error occurred while getting a connection ", e);
            return null;
        }
    }

    protected void testDataSource(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1000)) {
                throw new SQLException("Could not establish database connection.");
            }
        } catch (SQLException e) {
            log.log(Level.SEVERE, "An error occurred while testing the data source ", e);
        }
    }

    protected void setUUIDToStatement(PreparedStatement stmt, int index, UUID uuid) throws SQLException {
        if (this instanceof PostgreSqlStorage) {
            stmt.setObject(index, uuid);
        } else {
            stmt.setString(index, uuid.toString());
        }
    }

    protected void setJsonToStatement(PreparedStatement stmt, int index, JsonObject jsonObject) throws SQLException {
        if (this instanceof PostgreSqlStorage) {
            PGobject pgObject = new PGobject();
            pgObject.setType("jsonb");
            pgObject.setValue(jsonObject.toString());
            stmt.setObject(index, pgObject);
        } else {
            stmt.setString(index, jsonObject.toString());
        }
    }

    protected String kebabCaseToSnakeCase(String value) {
        return value.replace("-", "_");
    }
}
