package org.gi.gIEngine.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.gi.gIEngine.service.PlayerStatManager;

public class PlayerListener implements Listener {
    private final PlayerStatManager statManager;

    public PlayerListener(PlayerStatManager statManager){
        this.statManager = statManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        statManager.load(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event){
        statManager.unload(event.getPlayer());
    }
}
