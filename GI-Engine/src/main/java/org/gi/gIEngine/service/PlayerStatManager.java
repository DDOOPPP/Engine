package org.gi.gIEngine.service;

import org.bukkit.entity.Player;
import org.gi.gIEngine.model.PlayerStatHolder;
import org.gi.stat.IStatRegistry;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerStatManager {
    private final IStatRegistry statRegistry;
    private final Map<UUID, PlayerStatHolder> holders = new ConcurrentHashMap<>();

    public PlayerStatManager(IStatRegistry statRegistry){
        this.statRegistry = statRegistry;
    }

    public PlayerStatHolder load(Player player){
        return holders.computeIfAbsent(player.getUniqueId(), uuid -> {
            PlayerStatHolder holder = new PlayerStatHolder(statRegistry, player);
            holder.initializeAllStats();

            return holder;
        });
    }

    public void unload(Player player){
        PlayerStatHolder holder = holders.remove(player.getUniqueId());

        if (holder != null){
            holder.clearAllStats();
        }
    }

    public Optional<PlayerStatHolder> getHolder(UUID playerId){
        return Optional.ofNullable(holders.get(playerId));
    }

    public Optional<PlayerStatHolder> getHolder(Player player){
        return Optional.ofNullable(holders.get(player.getUniqueId()));
    }

    public PlayerStatHolder getOrLoad(Player player){
        return holders.computeIfAbsent(player.getUniqueId(), uuid -> {
            PlayerStatHolder holder = new PlayerStatHolder(statRegistry, player);
            holder.initializeAllStats();
            return holder;
        });
    }

    /**
     * 모든 플레이어 언로드 (서버 종료 시점 호출)
     * */
    public void unloadAll(){
        for (PlayerStatHolder holder : holders.values()){
            holder.clearAllStats();
        }
        holders.clear();
    }

    public int getLoadedPlayerCount(){
        return holders.size();
    }
}
