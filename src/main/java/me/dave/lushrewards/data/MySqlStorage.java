package me.dave.lushrewards.data;

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

public class MySqlStorage implements Storage<RewardUser, UUID> {
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
    public RewardUser load(UUID uuid) {
        try (Connection conn = conn(); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM lushrewards_data WHERE uniqueId = ?;")) {
            stmt.setString(1, uuid.toString());
            ResultSet resultSet = stmt.executeQuery();
            if (resultSet.next()) {
                return new RewardUser(
                    uuid,
                    resultSet.getString("username"),
                    resultSet.getInt("minutes-played")
                );
            } else {
                return new RewardUser(uuid, null, 0);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void save(RewardUser rewardUser) {
        try (Connection conn = conn(); PreparedStatement stmt = conn.prepareStatement("REPLACE INTO lushrewards_data(uuid, username, minutes-played) VALUES(?, ?, ?);")) {
            stmt.setString(1, rewardUser.getUniqueId().toString());
            stmt.setString(2, rewardUser.getUsername());
            stmt.setInt(3, rewardUser.getMinutesPlayed());
            stmt.executeUpdate();
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
