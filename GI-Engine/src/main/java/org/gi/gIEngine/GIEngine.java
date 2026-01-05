package org.gi.gIEngine;

import org.bukkit.plugin.java.JavaPlugin;
import org.gi.EngineAPI;
import org.gi.damage.DamageCalculator;
import org.gi.damage.IDamageCalculator;
import org.gi.gIEngine.command.CommandCore;
import org.gi.gIEngine.listener.DamageListener;
import org.gi.gIEngine.listener.PlayerListener;
import org.gi.gIEngine.service.PlayerStatManager;
import org.gi.stat.IStatRegistry;
import org.gi.stat.StatRegistry;

public final class GIEngine extends JavaPlugin {
    private static GIEngine plugin;

    private IStatRegistry statRegistry;
    private IDamageCalculator damageCalculator;
    private PlayerStatManager playerStatManager;
    private StatLoader statLoader;

    @Override
    public void onEnable() {
        plugin = this;

        statRegistry = new StatRegistry();
        playerStatManager = new PlayerStatManager(statRegistry);
        statLoader = new StatLoader(plugin,statRegistry);
        damageCalculator = new DamageCalculator();

        var result = EngineAPI.initialize(statRegistry,damageCalculator);
        if (!result.isSuccess()){
            getLogger().severe(result.getMessage());
            return;
        }

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

    }

    @Override
    public void onDisable() {
        playerStatManager.unloadAll();
        EngineAPI.shutdown();

        plugin = null;

        getLogger().info("GI-Engine Shutdown");
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

}
