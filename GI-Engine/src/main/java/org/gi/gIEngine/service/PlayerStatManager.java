package org.gi.gIEngine.service;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.gi.Result;
import org.gi.gIEngine.GIEngine;
import org.gi.gIEngine.model.PlayerStatHolder;
import org.gi.stat.IStatInstance;
import org.gi.stat.IStatModifier;
import org.gi.stat.IStatRegistry;
import org.gi.storage.IPlayerDataStorage;
import org.gi.storage.PlayerStatData;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class PlayerStatManager {
    private final IStatRegistry statRegistry;
    private final Map<UUID, PlayerStatHolder> holders = new ConcurrentHashMap<>();
    private final Logger logger;
    private final IPlayerDataStorage storage;
    private static final String PERMANENT_PREFIX = "permanent:";
    private final Set<UUID> loadingPlayers = ConcurrentHashMap.newKeySet();

    public PlayerStatManager(IStatRegistry statRegistry){
        this.statRegistry = statRegistry;
        logger = Bukkit.getLogger();
        storage = GIEngine.getInstance().getStorage();
    }

    public boolean isLoading(UUID playerId){
        return loadingPlayers.contains(playerId);
    }

    // public PlayerStatHolder load(Player player){
    //     UUID uuid = player.getUniqueId();

    //     PlayerStatHolder holder = new PlayerStatHolder(statRegistry, player);
    //     holder.initializeAllStats();

    //     if (storage != null && storage.isConnected()){
    //         try {
    //             storage.load(uuid)
    //                     .thenAccept(opt -> opt.ifPresent(data -> applyLoadedData(holder, data)))
    //                     .join();
    //         } catch (Exception e) {
    //             logger.warning("Failed to load player data: " + e.getMessage());
    //         }
    //     }

    //     holders.put(uuid, holder);
    //     return holder;
    // }

    public void load (Player player) {
        UUID uuid = player.getUniqueId();

        loadingPlayers.add(uuid);

        PlayerStatHolder holder = new PlayerStatHolder(statRegistry, player);
        holder.initializeAllStats();;

        holders.put(uuid, holder);

        if (storageAvailable()) {
            storage.load(uuid).thenAccept(opt -> {
                opt.ifPresent(data -> applyLoadedData(holder, data));
                loadingPlayers.remove(uuid);
                logger.info("Loaded player data: "+player.getName());
            }).exceptionally(e -> {
                logger.warning("Failed to load:"  +e.getMessage());
                loadingPlayers.remove(uuid);
                return null;
            });
        }else{
            loadingPlayers.remove(uuid);
        }
    }

    private void applyLoadedData(PlayerStatHolder holder,PlayerStatData data){
        //base값 적용
        for (Map.Entry<String,Double> entry : data.getBaseValues().entrySet()){
            holder.setBase(entry.getKey(), entry.getValue());
        }

        //modifier 적용
        if (data.getPermanentModifiers() != null){
            for (IStatModifier modifier : data.getPermanentModifiers()){
                holder.addModifier(modifier);
            }
        }
    }

    public void unload(Player player){
        UUID uuid = player.getUniqueId();
        PlayerStatHolder holder = holders.remove(player.getUniqueId());

        if (holder != null){
            if (storageAvailable()){
                save(holder);
            }
            holder.clearAllStats();
        }
    }

    public Optional<PlayerStatHolder> getHolder(UUID playerId){
        return Optional.ofNullable(holders.get(playerId));
    }

    public Optional<PlayerStatHolder> getHolder(Player player){
        return Optional.ofNullable(holders.get(player.getUniqueId()));
    }

    /**
     * 블로킹 없이 홀더 반환. 없거나 로딩 중이면 empty.
     */
    public Optional<PlayerStatHolder> getHolderIfReady(UUID playerId) {
        if (loadingPlayers.contains(playerId)) {
            return Optional.empty();
        }
        return Optional.ofNullable(holders.get(playerId));
    }

    public Optional<PlayerStatHolder> getHolderIfReady(Player player) {
        return getHolderIfReady(player.getUniqueId());
    }

    /**
     * 모든 플레이어 언로드 (서버 종료 시점 호출)
     * */
    public void unloadAll(){
        try{
            saveAll().get(10, TimeUnit.SECONDS);
        }catch (Exception e){
            logger.warning("Failed to save all player data: " + e.getMessage());
        }

        for (PlayerStatHolder holder : holders.values()) {
            holder.clearAllStats();
        }
        holders.clear();
    }

    public int getLoadedPlayerCount(){
        return holders.size();
    }

    public CompletableFuture<Void> save(PlayerStatHolder holder){
        return saveRetry(holder, 3);
    }

    private CompletableFuture<Void> saveRetry(PlayerStatHolder holder, int retryCount){
        if (!storageAvailable()){
            return CompletableFuture.completedFuture(null);
        }

        Map<String,Double> baseValues = new HashMap<>();
        for (IStatInstance instance : holder.getAllIStatInstances()){
            baseValues.put(instance.getStat().getID(), instance.getBase());
        }

        List<IStatModifier> permanentModifiers = holder.getAllIStatInstances().stream()
                .flatMap(instance -> instance.getActiveModifiers().stream())
                .filter(m -> m.getSource().startsWith(PERMANENT_PREFIX))
                .collect(Collectors.toList());

        PlayerStatData data = PlayerStatData.builder()
                .playerUUID(holder.getUUID())
                .baseValues(baseValues)
                .permanentModifiers(permanentModifiers)
                .build();

        return storage.save(data)
                .thenAccept(result -> {
                    if (result.isSuccess()){
                        logger.info("sava PlayerData: "+data.getPlayerUUID());
                    }
                }).exceptionally(e -> {
                    if (retryCount > 0){
                        logger.warning("Failed to save PlayerData: "+data.getPlayerUUID()+", retrying...");
                        logger.warning("retrying... leftCount: "+(retryCount-1));
                        saveRetry(holder, retryCount-1);
                    }else{
                        logger.warning("Failed to save PlayerData: "+data.getPlayerUUID());
                    }
                    return null;
                });
    }

    public CompletableFuture<Void> saveAll() {
        List<CompletableFuture<Void>> futures = holders.values().stream()
                .map(this::save)
                .toList();
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private boolean storageAvailable(){
        if (storage != null && storage.isConnected()){
            return true;
        }
        logger.warning("Storage is not connected");
        return false;
    }
}
