package org.gi.stat;

import org.gi.listener.IStatChangeListener;
import org.gi.stat.enums.HolderType;
import org.gi.stat.enums.ScalingType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class StatHolder implements IStatHolder{
    protected final IStatRegistry statRegistry;
    private final Map<String, IStatInstance> statInstances = new ConcurrentHashMap<>();
    private final List<IStatChangeListener> statChangeListeners = new ArrayList<>();


    public StatHolder(IStatRegistry statRegistry){
        this.statRegistry = Objects.requireNonNull(statRegistry,"StatRegistry cannot be null");
    }

    @Override
    public abstract UUID getUUID();

    @Override
    public abstract HolderType getType();

    @Override
    public abstract String getName();

    @Override
    public Optional<IStatInstance> getIStatInstance(String statId) {
        if (statId == null){
            return Optional.empty();
        }
        return Optional.ofNullable(statInstances.get(statId.toLowerCase()));
    }

    @Override
    public IStatInstance getOrCreateIStatInstance(String statId) {
        if (statId == null || statId.isBlank()){
            throw new IllegalArgumentException("StatId cannot be null or blank");
        }
        String key = statId.toLowerCase();

        return statInstances.computeIfAbsent(key, id ->{
            IStat stat = statRegistry.get(id).orElseThrow(
                    () -> new IllegalArgumentException("Stat '" + id + "' does not exist in registry"));

            return new StatInstance(stat, this);
        });
    }

    @Override
    public Collection<IStatInstance> getAllIStatInstances() {
        return Collections.unmodifiableCollection(statInstances.values());
    }

    @Override
    public boolean hasIStatInstance(String statId) {
        if (statId == null){
            return false;
        }
        return statInstances.containsKey(statId.toLowerCase());
    }

    @Override
    public int removeAllModifiersBySource(String source) {
        if (source == null){
            return 0;
        }

        return statInstances.values().stream()
                .mapToInt(instance -> instance.removeModifierBySource(source).size())
                .sum();
    }

    @Override
    public int cleanAllExpiredModifiers(){
        return statInstances.values().stream()
                .mapToInt(instance -> instance.cleanExpiredModifiers())
                .sum();
    }

    @Override
    public void recalculateAllStat(){
        for (IStatInstance instance : statInstances.values()){
            double oldValue = instance.getFinal();
            instance.inValidateCache();
            double newValue = instance.getFinal();

            if (oldValue != newValue){
                notifyStatChange(instance.getStat().getID(), oldValue, newValue);
            }
        }
    }

    @Override
    public void addStatChangeListener(IStatChangeListener listener){
        if (listener != null && !statChangeListeners.contains(listener)){
            statChangeListeners.add(listener);
        }
    }

    @Override
    public void removeStatChangeListener(IStatChangeListener listener){
        statChangeListeners.remove(listener);
    }

    protected void notifyStatChange(String statId, double oldValue, double newValue){
        for (IStatChangeListener listener : statChangeListeners){
            try{
                listener.onStatChange(this, statId, oldValue, newValue);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    public void initializeAllStats(){
        for (IStat stat: statRegistry.getAll()){
            getOrCreateIStatInstance(stat.getID());
        }
    }

    public void clearAllStats(){
        statInstances.clear();
    }

    public String getStatSummary() { //스탯 요약
        StringBuilder sb = new StringBuilder();
        sb.append("=== ").append(getName()).append(" Stats ===\n");

        for (IStatInstance instance : statInstances.values()) {
            IStat stat = instance.getStat();
            double value = instance.getFormatValue();

            sb.append(stat.getDisplayName()).append(": ");

            if (stat.isPercent()) {
                sb.append(String.format("%.1f%%", value * 100));
            } else if (stat.getScalingType() == ScalingType.INTEGER) {
                sb.append((int) value);
            } else {
                sb.append(String.format("%.2f", value));
            }

            sb.append(" (Base: ").append(instance.getBase());
            sb.append(", +").append(instance.getTotalFlat()).append(" flat");
            sb.append(", +").append(String.format("%.1f%%", instance.getTotalPercent() * 100));
            sb.append(")\n");
        }

        return sb.toString();
    }
}
