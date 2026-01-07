package org.gi.gIEngine;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.gi.EngineAPI;
import org.gi.damage.DamageCalculator;
import org.gi.damage.IDamageCalculator;
import org.gi.gIEngine.command.CommandCore;
import org.gi.gIEngine.listener.DamageListener;
import org.gi.gIEngine.listener.PlayerListener;
import org.gi.gIEngine.service.PlayerStatManager;
import org.gi.stat.IStatRegistry;
import org.gi.stat.StatRegistry;
import org.gi.storage.*;

import java.io.File;

import static org.gi.storage.StorageType.SQLITE;

public final class GIEngine extends JavaPlugin {
    private static GIEngine plugin;
    
    private GIConfig config;
    private IStatRegistry statRegistry;
    private IDamageCalculator damageCalculator;
    private PlayerStatManager playerStatManager;
    private StatLoader statLoader;
    private IPlayerDataStorage storage;
    private BukkitTask autoSaveTask;

    @Override
    public void onEnable() {
        plugin = this;
        this.saveDefaultConfig();
        config = new GIConfig(new File(plugin.getDataFolder(), "config.yml"));

        statRegistry = new StatRegistry();
        playerStatManager = new PlayerStatManager(statRegistry);
        statLoader = new StatLoader(plugin,statRegistry);
        damageCalculator = new DamageCalculator();

        var result = EngineAPI.initialize(statRegistry,damageCalculator);
        if (!result.isSuccess()){
            getLogger().severe(result.getMessage());
            return;
        }
        storage = initializeStorage();
        getLogger().info(statLoader.load().getMessage());

        getServer().getPluginManager().registerEvents(
                new PlayerListener(playerStatManager),
                this
        );

        getServer().getPluginManager().registerEvents(
                new DamageListener(playerStatManager,damageCalculator),
                this
        );

        getLogger().info("GI-Engine Initialized");

        getCommand("stat").setExecutor(new CommandCore(statRegistry,statLoader,damageCalculator));
        getCommand("stat").setTabCompleter(new CommandCore(statRegistry,statLoader,damageCalculator));
        getCommand("damage").setExecutor(new CommandCore(statRegistry,statLoader,damageCalculator));
        getCommand("damage").setTabCompleter(new CommandCore(statRegistry,statLoader,damageCalculator));

        startAutoSaveTask();

        // 이미 접속한 플레이어 로드 (리로드 대응)
        Bukkit.getOnlinePlayers().forEach(playerStatManager::load);
    }

    @Override
    public void onDisable() {
        // 자동 저장 태스크 중지
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
        }

        // 모든 플레이어 저장 및 언로드
        if (playerStatManager != null) {
            playerStatManager.unloadAll();
        }

        // 저장소 종료
        if (storage != null) {
            storage.shutdown();
        }

        // API 종료
        EngineAPI.shutdown();

        plugin = null;
        getLogger().info("GI-Engine disabled!");
    }



    private IPlayerDataStorage initializeStorage(){
        String typeStr = config.getString("storage.type").toUpperCase();

        StorageType type;

        try{
            type = StorageType.valueOf(typeStr);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Unknown storage type: " + typeStr + ". Using SQLITE.");
            type = StorageType.MYSQL;
        }

        return switch (type){
            case SQLITE -> {
                String fileName = config.getString("storage.sqlite.file");
                if (fileName == null || fileName.isEmpty()){
                    getLogger().warning("Not a valid storage file. Using SQLITE.");
                    fileName = "player_data.db";
                    getLogger().warning("create Default DB");
                }
                yield new SQLiteStorage(getLogger(), getDataFolder(), fileName);
            }
            case MYSQL -> {
                ConfigurationSection section = config.getSection("storage.mysql");

                String host = section.getString("host");
                int port = section.getInt("port");
                String database = section.getString("database");
                String user = section.getString("user");
                String password = section.getString("password");

                long connectionTimeOut = section.getLong("connection-timeout");
                long idleTimeOut = section.getLong("idle-timeout");
                long maxLifetime = section.getLong("max-life-time");
                long validationTimeOut = section.getLong("validation-timeout");
                long keepAliveTimeout = section.getLong("keep-alive-timeout");
                int maxPoolSize = section.getInt("max-pool-size");

                MySQLSetting setting = new MySQLSetting(
                        host,port,user,password,database,connectionTimeOut,idleTimeOut,maxLifetime,validationTimeOut,keepAliveTimeout,maxPoolSize
                );

                yield new MySQLStorage(getLogger(),setting);
            }
        };
    }

    private void startAutoSaveTask(){
        int interval = config.getInt("storage.auto-save-interval");

        if (interval <= 0){
            getLogger().info("Auto-save disabled.");
            return;
        }

        long intervalTicks = interval * 20L;

        autoSaveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            if (playerStatManager != null){
                playerStatManager.saveAll();

                if (config.getBoolean("storage.debug")){
                    getLogger().info("Auto-saved " + playerStatManager.getLoadedPlayerCount() + " players.");
                }
            }
        }, intervalTicks, intervalTicks);

        getLogger().info("Auto-save enabled: every " + interval + " seconds.");
    }

    public static GIEngine getInstance() {
        return plugin;
    }

    public IStatRegistry getStatRegistry() {
        return statRegistry;
    }

    public IDamageCalculator getDamageCalculator() {
        return damageCalculator;
    }

    public PlayerStatManager getPlayerStatManager() {
        return playerStatManager;
    }

    public StatLoader getStatLoader() {
        return statLoader;
    }

    public IPlayerDataStorage getStorage() {
        return storage;
    }
}
