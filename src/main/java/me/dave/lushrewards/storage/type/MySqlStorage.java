package me.dave.lushrewards.storage.type;

import com.mysql.cj.jdbc.MysqlConnectionPoolDataSource;
import com.mysql.cj.jdbc.MysqlDataSource;
import me.dave.lushrewards.LushRewards;
import me.dave.lushrewards.storage.StorageManager;
import me.dave.lushrewards.storage.StorageObject;
import me.dave.lushrewards.storage.StorageProvider;
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
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class MySqlStorage implements Storage<StorageObject, StorageManager.ProviderId> {
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
    public StorageObject load(StorageManager.ProviderId providerId) {
        String key = providerId.key();
        String providerName = providerId.providerName();
        String tableName = providerName != null ? "_" + providerName : "_data";

        StorageProvider<?> storageProvider = LushRewards.getInstance().getStorageManager().getStorageProvider(providerName);
        if (storageProvider != null) {
            StorageObject storageObject = new StorageObject(key, providerId.providerName());
            try (Connection conn = conn(); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM " + tableName + " WHERE uniqueId = ?;")) {
                stmt.setString(1, providerId.key());
                ResultSet resultSet = stmt.executeQuery();
                if (resultSet.next()) {
                    storageProvider.getMethodHolders().forEach((id, methodHolder) -> {
                        try {
                            if (methodHolder.getStorageType().equals(String.class)) {
                                storageObject.set(id, resultSet.getString(id), methodHolder.getConvertToLocalMethod());
                            } else if (methodHolder.getStorageType().equals(Integer.class)) {
                                storageObject.set(id, resultSet.getInt(id), methodHolder.getConvertToLocalMethod());
                            } else if (methodHolder.getStorageType().equals(Boolean.class)) {
                                storageObject.set(id, resultSet.getBoolean(id), methodHolder.getConvertToLocalMethod());
                            } else if (methodHolder.getStorageType().equals(Double.class)) {
                                storageObject.set(id, resultSet.getDouble(id), methodHolder.getConvertToLocalMethod());
                            } else if (methodHolder.getStorageType().equals(Long.class)) {
                                storageObject.set(id, resultSet.getLong(id), methodHolder.getConvertToLocalMethod());
                            }
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                    });
                    return storageObject;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    @Override
    public void save(StorageObject storageObject) {
        String providerName = storageObject.getProviderName();
        String tableName = providerName != null ? "_" + providerName : "_data";

        StorageProvider<?> storageProvider = LushRewards.getInstance().getStorageManager().getStorageProvider(providerName);
        if (storageProvider != null) {
            HashMap<String, ? extends StorageProvider.MethodHolder<?, ?, ?>> methodHolders = storageProvider.getMethodHolders();
            List<String> ids = new ArrayList<>(methodHolders.keySet());
            ids.add(0, "uniqueId");
            String columns = String.join(", ", ids);

            String query = "REPLACE INTO lushrewards" + tableName + "(" + columns + ") VALUES(" + columns.length() + ");";
            try (Connection conn = conn(); PreparedStatement stmt = conn.prepareStatement(query)) {
                for (int i = 0; i < columns.length(); i++) {
                    String id = ids.get(i);
                    if (id.equals("uniqueId")) {
                        stmt.setString(i + 1, storageObject.getKey());
                        continue;
                    }

                    StorageProvider.MethodHolder<?, ?, ?> methodHolder = methodHolders.get(id);
                    if (methodHolder.getStorageType().equals(String.class)) {
                        stmt.setString(i + 1, storageObject.getRemoteString(id));
                    } else if (methodHolder.getStorageType().equals(Integer.class)) {
                        stmt.setInt(i + 1, storageObject.getRemoteInteger(id));
                    } else if (methodHolder.getStorageType().equals(Boolean.class)) {
                        stmt.setBoolean(i + 1, storageObject.getRemoteBoolean(id));
                    } else if (methodHolder.getStorageType().equals(Double.class)) {
                        stmt.setDouble(i + 1, storageObject.getRemoteDouble(id));
                    } else if (methodHolder.getStorageType().equals(Long.class)) {
                        stmt.setLong(i + 1, storageObject.getRemoteLong(id));
                    }
                }

                stmt.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
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
