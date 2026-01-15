package org.gi.storage;

import com.zaxxer.hikari.HikariDataSource;
import org.gi.Result;
import org.gi.builder.StatModifierBuilder;
import org.gi.stat.IStatModifier;
import org.gi.stat.enums.ModifierType;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public abstract class AbstractStorage implements IPlayerDataStorage{
    protected final Logger logger;
    protected final ExecutorService executor;
    protected Connection connection;

    protected static final String PERMANENT_PREFIX = "permanent:";

    public AbstractStorage(Logger logger) {
        this.logger = logger;
        this.executor = Executors.newFixedThreadPool(2);
    }

    protected abstract Connection createConnection() throws SQLException;

    protected abstract String getCreateStatsTableSQL();
    protected abstract String getCreateModifiersTableSQL();

    @Override
    public Result initialize(){
        try{
            connection = createConnection();

            createTables();
            logger.info("Storage initialized: " + getType());
            return Result.SUCCESS;
        }catch (SQLException e){
            logger.severe("Failed to initialize storage: " + e.getMessage());
            return Result.Exception(e);
        }
    }

    private void createTables() throws SQLException{
        try(Statement stmt = connection.createStatement()){
            stmt.execute(getCreateStatsTableSQL());
            stmt.execute(getCreateModifiersTableSQL());
        }
    }

    @Override
    public void shutdown(){
        executor.shutdown();

        try{
            if (connection != null && !connection.isClosed()){
                connection.close();
            }
        } catch (SQLException e) {
            logger.warning("Error closing connection: " + e.getMessage());
        }
    }

    @Override
    public boolean isConnected(){
        try{
            return connection != null && !connection.isClosed();
        }catch (SQLException e){
            return false;
        }
    }

    @Override
    public CompletableFuture<Result> save(PlayerStatData data){
        return CompletableFuture.supplyAsync(() -> {
            try{
                saveBaseValues(data);
                savePermanentModifiers(data);
                return Result.SUCCESS;
            } catch (SQLException e) {
                logger.severe("Failed to save player data: " + e.getMessage());
                return Result.Exception(e);
            }
        }, executor);
    }

    public void saveBaseValues(PlayerStatData data) throws SQLException{
        String sql = "REPLACE INTO player_stats (player_uuid, stat_id, base_value) VALUES (?,?,?)";

        try(PreparedStatement stmt = connection.prepareStatement(sql)){
            for(Map.Entry<String,Double> entry : data.getBaseValues().entrySet()){
                stmt.setString(1, data.getPlayerUUID().toString());
                stmt.setString(2, entry.getKey());
                stmt.setDouble(3, entry.getValue());
                stmt.addBatch();
            }

            stmt.executeBatch();
        }
    }
    //기존의 영구 Modifier 삭제
    private void savePermanentModifiers(PlayerStatData data) throws SQLException{
        String deleteSQL = "DELETE FROM player_modifiers  WHERE player_uuid = ?";

        try(PreparedStatement stmt = connection.prepareStatement(deleteSQL)){
            stmt.setString(1, data.getPlayerUUID().toString());
            stmt.executeUpdate();
        }

        if (data.getPermanentModifiers() == null || data.getPermanentModifiers().isEmpty()){
            return;
        }

        String insertSQL = """
                INSERT INTO player_modifiers 
                (player_uuid, modifier_uuid, stat_id, source, display_name, type, value, priority, stackable, max_stacks) 
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try(PreparedStatement stmt = connection.prepareStatement(insertSQL)){
            for (IStatModifier modifier : data.getPermanentModifiers()){
                if (!modifier.getSource().startsWith(PERMANENT_PREFIX)){
                    continue;
                }

                stmt.setString(1, data.getPlayerUUID().toString());
                stmt.setString(2, modifier.getID().toString());
                stmt.setString(3, modifier.getStatID());
                stmt.setString(4, modifier.getSource());
                stmt.setString(5, modifier.getSource()); // displayName 없으면 source 사용
                stmt.setString(6, modifier.getType().name());
                stmt.setDouble(7, modifier.getValue());
                stmt.setInt(8, modifier.getPriority());
                stmt.setBoolean(9, modifier.isStackable());
                stmt.setInt(10, modifier.getStackCount());
                stmt.addBatch();
            }
            stmt.executeBatch();
        }
    }

    @Override
    public CompletableFuture<Optional<PlayerStatData>> load(UUID playerUUID){
        return CompletableFuture.supplyAsync(() -> {
            try{
                Map<String,Double> baseValues = loadBaseValues(playerUUID);
                List<IStatModifier> modifiers = loadPermanentModifiers(playerUUID);

                if (baseValues.isEmpty() && modifiers.isEmpty()){
                    return Optional.empty();
                }

                PlayerStatData statData = PlayerStatData.builder()
                        .playerUUID(playerUUID)
                        .baseValues(baseValues)
                        .permanentModifiers(modifiers)
                        .build();

                return Optional.of(statData);
            } catch (SQLException e) {
                logger.severe("Failed to load player data: " + e.getMessage());
                return Optional.empty();
            }
        }, executor);
    }

    private Map<String,Double> loadBaseValues(UUID playerUUID) throws SQLException{
        Map<String,Double> values = new HashMap<>();

        String sql = "SELECT stat_id, base_value FROM player_stats WHERE player_uuid = ?";

        try(PreparedStatement stmt = connection.prepareStatement(sql)){
            stmt.setString(1, playerUUID.toString());

            try(ResultSet rs = stmt.executeQuery()){
                while (rs.next()){
                    values.put(rs.getString("stat_id"), rs.getDouble("base_value"));
                }
            }
        }
        return values;
    }

    private List<IStatModifier> loadPermanentModifiers(UUID playerUUID) throws SQLException{
        List<IStatModifier> modifiers = new ArrayList<>();

        String sql = "SELECT * FROM player_modifiers WHERE player_uuid = ?";

        try(PreparedStatement stmt = connection.prepareStatement(sql)){
            stmt.setString(1, playerUUID.toString());

            try(ResultSet rs = stmt.executeQuery()){
                while (rs.next()){
                    IStatModifier modifier = StatModifierBuilder.create()
                            .uniqueId(UUID.fromString(rs.getString("modifier_uuid")))
                            .statId(rs.getString("stat_id"))
                            .source(rs.getString("source"))
                            .displayName(rs.getString("display_name"))
                            .type(ModifierType.valueOf(rs.getString("type")))
                            .value(rs.getDouble("value"))
                            .priority(rs.getInt("priority"))
                            .stackable(rs.getBoolean("stackable"))
                            .maxStacks(rs.getInt("max_stacks"))
                            .permanent()
                            .build();

                    modifiers.add(modifier);
                }
            }
        }
        return modifiers;
    }

    @Override
    public CompletableFuture<Result> delete (UUID playerUUID){
        return CompletableFuture.supplyAsync(() -> {
            try{
                String deleteStats = "DELETE FROM player_stats WHERE player_uuid = ?";
                String deleteModifiers = "DELETE FROM player_modifiers WHERE player_uuid = ?";

                try(PreparedStatement statement = connection.prepareStatement(deleteStats)){
                    statement.setString(1, playerUUID.toString());
                    statement.executeUpdate();
                }

                try(PreparedStatement statement = connection.prepareStatement(deleteModifiers)){
                    statement.setString(1, playerUUID.toString());
                    statement.executeUpdate();
                }

                return Result.SUCCESS;
            }catch (SQLException e){
                logger.severe("Failed to delete stats: " + e.getMessage());
                return Result.Exception(e);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> exists(UUID playerUUID){
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT 1 FROM player_stats WHERE player_uuid = ? LIMIT 1";

            try(PreparedStatement statement = connection.prepareStatement(sql)){
                statement.setString(1, playerUUID.toString());

                try(ResultSet rs = statement.executeQuery()){
                    return rs.next();
                }
            } catch (SQLException e) {
                logger.severe("Failed to find stats: " + e.getMessage());
                return false;
            }
        });
    }
}
