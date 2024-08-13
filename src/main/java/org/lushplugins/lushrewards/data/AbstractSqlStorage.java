package org.lushplugins.lushrewards.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.enchantedskies.EnchantedStorage.Storage;
import org.lushplugins.lushrewards.LushRewards;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public abstract class AbstractSqlStorage implements Storage<DataManager.StorageData, DataManager.StorageLocation> {
    protected static final String TABLE_NAME = "lushrewards_users";
    protected static final String MODULES_TABLE_NAME = "lushrewards_users_modules";

    protected final DataSource dataSource;

    protected AbstractSqlStorage(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    protected abstract String getUpsertStatement(String table, String column);

    protected abstract void assertJsonColumn(String table, String column);

    protected abstract void assertColumn(String table, String column, String type);

    protected abstract void setUUIDToStatement(PreparedStatement stmt, int index, UUID uuid) throws SQLException;

    protected abstract void setJsonToStatement(PreparedStatement stmt, int index, JsonObject jsonObject) throws SQLException;

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
            LushRewards.getInstance().log(Level.SEVERE, "An error occurred while loading data for ", e);
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
            LushRewards.getInstance().log(Level.SEVERE, "An error occurred while saving data for ", e);
        }
    }


    protected void assertTable(String table) {
        String statement = String.format("CREATE TABLE IF NOT EXISTS %s (uuid UUID NOT NULL, PRIMARY KEY (uuid));", table);
        try (Connection conn = conn();
             PreparedStatement stmt = conn.prepareStatement(statement)
        ) {
            stmt.execute();
        } catch (SQLException e) {
            LushRewards.getInstance().log(Level.SEVERE, "An error occurred while creating table ", e);
        }
    }

    protected Connection conn() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            LushRewards.getInstance().log(Level.SEVERE, "An error occurred while getting a connection ", e);
            return null;
        }
    }

    protected void testDataSource(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1000)) {
                throw new SQLException("Could not establish database connection.");
            }
        } catch (SQLException e) {
            LushRewards.getInstance().log(Level.SEVERE, "An error occurred while testing the data source ", e);
        }
    }

    protected String kebabCaseToSnakeCase(String value) {
        return value.replace("-", "_");
    }
}
