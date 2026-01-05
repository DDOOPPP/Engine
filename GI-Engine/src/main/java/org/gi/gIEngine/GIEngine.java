package org.gi.gIEngine;

import org.bukkit.plugin.java.JavaPlugin;
import org.gi.EngineAPI;
import org.gi.gIEngine.command.CommandCore;
import org.gi.gIEngine.listener.PlayerListener;
import org.gi.gIEngine.service.PlayerStatManager;
import org.gi.stat.IStatRegistry;
import org.gi.stat.StatRegistry;

public final class GIEngine extends JavaPlugin {
    private static GIEngine plugin;

    private IStatRegistry statRegistry;
    private PlayerStatManager playerStatManager;
    private StatLoader statLoader;

    @Override
    public void onEnable() {
        plugin = this;

        statRegistry = new StatRegistry();
        playerStatManager = new PlayerStatManager(statRegistry);
        statLoader = new StatLoader(plugin,statRegistry);

        var result = EngineAPI.initialize(statRegistry);
        if (!result.isSuccess()){
            getLogger().severe(result.getMessage());
            return;
        }

        getLogger().info(statLoader.load().getMessage());

        getServer().getPluginManager().registerEvents(
                new PlayerListener(playerStatManager),
                this
        );

        getLogger().info("GI-Engine Initialized");

        getCommand("stat").setExecutor(new CommandCore(statRegistry,statLoader));
        getCommand("stat").setTabCompleter(new CommandCore(statRegistry,statLoader));
    }

    @Override
    public void onDisable() {
        playerStatManager.unloadAll();
        EngineAPI.shutdown();

        plugin = null;

        getLogger().info("GI-Engine Shutdown");
    }

    public static GIEngine getPlugin() {
        return plugin;
    }

}
