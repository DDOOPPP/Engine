package org.gi.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.gi.Result;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class MySQLStorage extends AbstractStorage{
    private MySQLSetting setting;
    private HikariDataSource dataSource = null;

    public MySQLStorage(Logger logger, MySQLSetting setting) {
        super(logger,setting.getMaximumPoolSize());
        this.setting = setting;
    }

    @Override
    public Result initialize(){
        if (setting == null){
            logger.severe("database setting is null");
            return Result.Error("Database setting is null");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(setting.getURL());
        config.setUsername(setting.getUser());
        config.setPassword(setting.getPassword());
        config.setMaximumPoolSize(setting.getMaximumPoolSize());
        config.setMinimumIdle(setting.getMaximumPoolSize());
        config.setValidationTimeout(setting.getValidationTimeout());
        config.setKeepaliveTime(setting.getKeepAliveTimeout());
        config.setIdleTimeout(setting.getIdleTimeout());
        config.setMaxLifetime(setting.getMaxLifetime());
        config.setConnectionTimeout(setting.getConnectionTimeout());
        config.setPoolName("GI-Engine-Pool");

        try {
            dataSource = new HikariDataSource(config);
        } catch (Exception e) {
            logger.severe("Failed to create MySQL connection pool: " + e.getMessage());
            return Result.Exception(e);
        }

        return super.initialize();
    }

    @Override
    protected Connection createConnection() throws SQLException {
        return getConnection();
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection();
             var stmt = conn.createStatement()) {
            stmt.execute(getCreateStatsTableSQL());
            stmt.execute(getCreateModifiersTableSQL());
        }
    }

    @Override
    protected String getCreateStatsTableSQL() {
        return """
            CREATE TABLE IF NOT EXISTS player_stats (
                player_uuid VARCHAR(36) NOT NULL,
                stat_id VARCHAR(64) NOT NULL,
                base_value DOUBLE NOT NULL,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                PRIMARY KEY (player_uuid, stat_id),
                INDEX idx_player (player_uuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
    }

    @Override
    protected String getCreateModifiersTableSQL() {
        return """
            CREATE TABLE IF NOT EXISTS player_modifiers (
                player_uuid VARCHAR(36) NOT NULL,
                modifier_uuid VARCHAR(36) NOT NULL,
                stat_id VARCHAR(64) NOT NULL,
                source VARCHAR(128) NOT NULL,
                display_name VARCHAR(128),
                type VARCHAR(16) NOT NULL,
                value DOUBLE NOT NULL,
                priority INT DEFAULT 0,
                stackable BOOLEAN DEFAULT TRUE,
                max_stacks INT DEFAULT -1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (player_uuid, modifier_uuid),
                INDEX idx_player (player_uuid),
                INDEX idx_stat (stat_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
    }

    @Override
    public void shutdown() {
        super.shutdown();

        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("MySQL connection pool closed");
        }
    }
    @Override
    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed() && dataSource.isRunning();
    }

    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public StorageType getType() {
        return StorageType.MYSQL;
    }

    public String getPoolStatus() {
        if (dataSource == null) return "DataSource is null";

        return String.format(
                "Pool[active=%d, idle=%d, waiting=%d, total=%d]",
                dataSource.getHikariPoolMXBean().getActiveConnections(),
                dataSource.getHikariPoolMXBean().getIdleConnections(),
                dataSource.getHikariPoolMXBean().getThreadsAwaitingConnection(),
                dataSource.getHikariPoolMXBean().getTotalConnections()
        );
    }
}
