package org.gi.stat;

import org.gi.stat.enums.ScalingType;
import org.gi.stat.enums.StatCategory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class StatRegistry implements IStatRegistry{
    private final Map<String, IStat> statMap = new ConcurrentHashMap<>();
    private volatile boolean locked = false;

    @Override
    public boolean register(IStat stat) {
        if (locked){
            throw new IllegalStateException("StatRegistry is locked");
        }
        if (stat == null){
            throw new IllegalArgumentException("Stat cannot be null");
        }
        if (stat.getID() == null || stat.getID().isBlank()){
            throw new IllegalArgumentException("Stat id cannot be null or blank");
        }
        return statMap.putIfAbsent(stat.getID().toLowerCase(), stat) == null;
    }

    @Override
    public Optional<IStat> unregister(String statId) {
        if (locked){
            throw new IllegalStateException("StatRegistry is locked");
        }
        if (statId == null || statId.isBlank()){
            return Optional.empty();
        }
        return Optional.ofNullable(statMap.remove(statId.toLowerCase()));
    }

    @Override
    public Optional<IStat> get(String statId) {
       if (statId == null){
           return Optional.empty();
       }
       return Optional.ofNullable(statMap.get(statId.toLowerCase()));
    }

    @Override
    public Collection<IStat> getAll() {
        return Collections.unmodifiableCollection(statMap.values());
    }

    @Override
    public boolean contains(String statId) {
        if (statId == null){
            return false;
        }
        return statMap.containsKey(statId.toLowerCase());
    }

    @Override
    public Collection<IStat> getStatsByCategory(StatCategory category) {
        if (category == null){
            return Collections.emptyList();
        }
        return statMap.values().stream()
                .filter(stat -> stat.getCategory() == category)
                .toList();
    }

    @Override
    public Collection<IStat> getStatsByScalingType(ScalingType scalingType) {
        if (scalingType == null){
            return Collections.emptyList();
        }
        return statMap.values().stream()
                .filter(stat -> stat.getScalingType() == scalingType)
                .toList();
    }

    @Override
    public int getStatCount() {
        return statMap.size();
    }

    @Override
    public void clear() {
        if (locked){
            throw new IllegalStateException("StatRegistry is locked");
        }
        statMap.clear();

    }

    @Override
    public void lock() {
        this.locked = true;
    }

    @Override
    public boolean isLocked() {
        return locked;
    }

    @Override
    public void unlock() {
        this.locked = false;
    }
}
