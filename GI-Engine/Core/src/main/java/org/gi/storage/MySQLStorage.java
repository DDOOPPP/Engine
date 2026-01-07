package org.gi.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

public class MySQLStorage extends AbstractStorage{
    private MySQLSetting setting;
    private HikariDataSource dataSource = null;
    private Connection connection;

    public MySQLStorage(Logger logger, MySQLSetting setting) {
        super(logger);
        this.setting = setting;
    }

    @Override
    protected Connection createConnection() throws SQLException {
        if (setting == null){
            logger.severe("database setting is null");
            logger.severe("check config.yml File");
            return null;
        }

        dataSource = new HikariDataSource();

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

        dataSource = new HikariDataSource(config);
        connection = dataSource.getConnection();

        return connection;
    }

    @Override
    protected String getCreateStatsTableSQL() {
        return """
            CREATE TABLE IF NOT EXISTS player_stats (
                player_uuid VARCHAR(36) NOT NULL,
                stat_id VARCHAR(64) NOT NULL,
                base_value DOUBLE NOT NULL,
                PRIMARY KEY (player_uuid, stat_id)
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
                PRIMARY KEY (player_uuid, modifier_uuid),
                INDEX idx_player (player_uuid)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
    }

    @Override
    public StorageType getType() {
        return StorageType.MYSQL;
    }
}
