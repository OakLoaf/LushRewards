package me.dave.lushrewards.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import org.enchantedskies.EnchantedStorage.Storage;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class MySqlStorage implements Storage<DataManager.StorageData, DataManager.StorageLocation> {
    private static final String TABLE_NAME = "lushrewards_users";
    private static final String MODULES_TABLE_NAME = "lushrewards_users_modules";

    private final MysqlDataSource dataSource;

    public MySqlStorage(String host, int port, String databaseName, String user, String password) {
        dataSource = initDataSource(host, port, databaseName, user, password);
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

        assertJsonColumn(table, column);

        try (Connection conn = conn(); PreparedStatement stmt = conn.prepareStatement("SELECT " + column + " FROM " + table + " WHERE uniqueId = ?;")) {
            stmt.setString(1, uuid.toString());

            ResultSet resultSet = stmt.executeQuery();
            String jsonRaw = resultSet.getString(column);
            JsonObject json = jsonRaw != null ? JsonParser.parseString(jsonRaw).getAsJsonObject() : null;

            return new DataManager.StorageData(uuid, module, json);
        } catch (SQLException e) {
            e.printStackTrace();
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

        assertJsonColumn(table, column);

        try (Connection conn = conn(); PreparedStatement stmt = conn.prepareStatement("REPLACE INTO " + table + "(uuid, " + column + ") VALUES(?, ?);")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, json.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void assertJsonColumn(String table, String column) {
        assertColumn(table, column, "JSON");
    }

    @SuppressWarnings("SameParameterValue")
    private void assertColumn(String table, String column, String type) {
        assertTable(table);
        String query = "ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS " + column + " " + type + ";";

        try (Connection conn = conn(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void assertTable(String table) {
        String query = "CREATE TABLE IF NOT EXISTS " + table + "(uuid CHAR(36) NOT NULL, PRIMARY KEY (uuid));";

        try (Connection conn = conn(); PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection conn() {
        try {
            return dataSource.getConnection();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }

    private MysqlDataSource initDataSource(String host, int port, String dbName, String user, String password) {
        MysqlDataSource dataSource = new MysqlConnectionPoolDataSource();
        dataSource.setServerName(host);
        dataSource.setPortNumber(port);
        dataSource.setDatabaseName(dbName);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        testDataSource(dataSource);
        return dataSource;
    }

    private void testDataSource(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            if (!conn.isValid(1000)) {
                throw new SQLException("Could not establish database connection.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
