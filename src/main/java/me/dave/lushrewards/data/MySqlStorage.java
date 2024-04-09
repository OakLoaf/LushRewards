package me.dave.lushrewards.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import me.dave.lushrewards.LushRewards;
import org.enchantedskies.EnchantedStorage.Storage;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MySqlStorage implements Storage<DataManager.StorageData, DataManager.StorageLocation> {
    private static final String TABLE_NAME = "lushrewards_users";

    private MysqlDataSource dataSource;

    public void setup(String host, int port, String databaseName, String user, String password) {
        dataSource = initDataSource(host, port, databaseName, user, password);

        String setup;
        try (InputStream in = LushRewards.getInstance().getResource("dbsetup.sql")) {
            setup = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        } catch (IOException e) {
            LushRewards.getInstance().getLogger().log(Level.SEVERE, "Could not read db setup file.", e);
            e.printStackTrace();
            return;
        }

        String[] queries = setup.split(";");
        for (String query : queries) {
            if (query.isEmpty()) {
                continue;
            }

            try (Connection conn = dataSource.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.execute();
            } catch (SQLException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    @Override
    public DataManager.StorageData load(DataManager.StorageLocation storageLocation) {
        UUID uuid = storageLocation.uuid();
        String module = storageLocation.moduleId();
        String column = module != null ? module + "_data" : "root_data";

        assertJsonColumn(TABLE_NAME, column);

        try (Connection conn = conn(); PreparedStatement stmt = conn.prepareStatement("SELECT " + column + " FROM " + TABLE_NAME + " WHERE uniqueId = ?;")) {
            stmt.setString(1, uuid.toString());

            ResultSet resultSet = stmt.executeQuery();
            JsonObject json = JsonParser.parseString(resultSet.getString(column)).getAsJsonObject();
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
        String column = module != null ? module + "_data" : "root_data";

        assertJsonColumn(TABLE_NAME, column);

        try (Connection conn = conn(); PreparedStatement stmt = conn.prepareStatement("REPLACE INTO " + TABLE_NAME + "(uuid, " + column + ") VALUES(?, ?);")) {
            stmt.setString(1, uuid.toString());
            stmt.setString(2, storageData.json().toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void assertJsonColumn(String table, String column) {
        assertColumn(table, column, "JSON");
    }

    private void assertColumn(String table, String column, String type) {
        String query = "ALTER TABLE " + table + " ADD COLUMN IF NOT EXISTS " + column + " " + type + ";";

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
