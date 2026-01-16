package org.gi.storage;

import org.gi.Result;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

public class SQLiteStorage extends AbstractStorage {
    private final File dataFolder;
    private final String fileName;
    private Connection connection;  // SQLite는 단일 커넥션 유지

    public SQLiteStorage(Logger logger, File dataFolder, String fileName) {
        super(logger);
        this.dataFolder = dataFolder;
        this.fileName = fileName;
    }

    public SQLiteStorage(Logger logger, File dataFolder) {
        this(logger, dataFolder, "player_data.db");
    }

    @Override
    public Result initialize() {
        try {
            // 폴더 생성
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }

            // 드라이버 로드
            Class.forName("org.sqlite.JDBC");

            // 커넥션 생성
            connection = createConnection();

            // 테이블 생성
            createTables();

            logger.info("SQLiteStorage initialized: " + fileName);
            return Result.SUCCESS;
        } catch (ClassNotFoundException e) {
            logger.severe("SQLite JDBC driver not found: " + e.getMessage());
            return Result.Error("SQLite driver not found");
        } catch (SQLException e) {
            logger.severe("Failed to initialize SQLiteStorage: " + e.getMessage());
            return Result.Exception(e);
        }
    }

    @Override
    protected Connection createConnection() throws SQLException {
        File dbFile = new File(dataFolder, fileName);
        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        Connection conn = DriverManager.getConnection(url);

        // SQLite 성능 최적화
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL");      // Write-Ahead Logging
            stmt.execute("PRAGMA synchronous=NORMAL");    // 동기화 수준
            stmt.execute("PRAGMA cache_size=10000");      // 캐시 크기
            stmt.execute("PRAGMA temp_store=MEMORY");     // 임시 저장소
        }

        return conn;
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(getCreateStatsTableSQL());
            stmt.execute(getCreateModifiersTableSQL());
        }
    }

    /**
     * SQLite는 단일 커넥션 반환
     * 커넥션이 닫혔으면 재생성
     */
    @Override
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = createConnection();
            logger.info("SQLite connection recreated");
        }
        return connection;
    }

    @Override
    public void shutdown() {
        // executor 종료
        super.shutdown();

        // 커넥션 종료
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("SQLite connection closed");
            }
        } catch (SQLException e) {
            logger.warning("Error closing SQLite connection: " + e.getMessage());
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    @Override
    protected String getCreateStatsTableSQL() {
        return """
            CREATE TABLE IF NOT EXISTS player_stats (
                player_uuid TEXT NOT NULL,
                stat_id TEXT NOT NULL,
                base_value REAL NOT NULL,
                updated_at TEXT DEFAULT CURRENT_TIMESTAMP,
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
                created_at TEXT DEFAULT CURRENT_TIMESTAMP,
                PRIMARY KEY (player_uuid, modifier_uuid)
            )
            """;
    }

    @Override
    public StorageType getType() {
        return StorageType.SQLITE;
    }

    /**
     * DB 파일 경로 반환 (디버깅용)
     */
    public String getDbPath() {
        return new File(dataFolder, fileName).getAbsolutePath();
    }
}