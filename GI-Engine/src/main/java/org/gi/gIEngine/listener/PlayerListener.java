package org.gi.gIEngine.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.gi.gIEngine.service.PlayerStatManager;

public class PlayerListener implements Listener {
    private final PlayerStatManager statManager;

    public PlayerListener(PlayerStatManager statManager){
        this.statManager = statManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        // 가장 먼저 로드 (다른 플러그인보다 먼저)
        statManager.load(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        // 가장 나중에 언로드 (저장 포함)
        statManager.unload(event.getPlayer());
    }
}
