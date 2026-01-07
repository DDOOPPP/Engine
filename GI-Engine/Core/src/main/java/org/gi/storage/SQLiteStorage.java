package org.gi.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

public class SQLiteStorage extends AbstractStorage {
    private final File dataFolder;
    private final String fileName;

    public SQLiteStorage(Logger logger, final File dataFolder, final String fileName) {
        super(logger);
        this.dataFolder = dataFolder;
        this.fileName = fileName;
    }

    public SQLiteStorage(Logger logger, File dataFolder) {
        this(logger, dataFolder, "player_data.db");
    }

    @Override
    protected Connection createConnection() throws SQLException {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, fileName);
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        return DriverManager.getConnection(url);
    }

    @Override
    protected String getCreateStatsTableSQL() {
        return """
            CREATE TABLE IF NOT EXISTS player_stats (
                player_uuid TEXT NOT NULL,
                stat_id TEXT NOT NULL,
                base_value REAL NOT NULL,
                PRIMARY KEY (player_uuid, stat_id)
            )
            """;
    }

    @Override
    protected String getCreateModifiersTableSQL() {
        return """
            CREATE TABLE IF NOT EXISTS player_modifiers (
                player_uuid TEXT NOT NULL,
                modifier_uuid TEXT NOT NULL,
                stat_id TEXT NOT NULL,
                source TEXT NOT NULL,
                display_name TEXT,
                type TEXT NOT NULL,
                value REAL NOT NULL,
                priority INTEGER DEFAULT 0,
                stackable INTEGER DEFAULT 1,
                max_stacks INTEGER DEFAULT -1,
                PRIMARY KEY (player_uuid, modifier_uuid)
            )
            """;
    }

    @Override
    public StorageType getType() {
        return StorageType.SQLITE;
    }
}
