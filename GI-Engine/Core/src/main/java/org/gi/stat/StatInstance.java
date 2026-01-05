package org.gi.stat;

import org.gi.stat.enums.ModifierType;
import org.gi.stat.enums.ScalingType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class StatInstance implements IStatInstance{
    private final IStat stat;
    private final IStatHolder holder;
    private final Map<UUID, IStatModifier> modifiers= new ConcurrentHashMap<>();

    private double baseValue;

    private volatile boolean cacheValid = false;
    private double cacheFinalValue;
    private double cacheTotalFlat;
    private double cacheTotalPercent;

    public StatInstance(IStat stat, IStatHolder holder) {
        this.stat = Objects.requireNonNull(stat, "Stat cannot be null");
        this.holder = Objects.requireNonNull(holder, "Holder cannot be null");
        this.baseValue = stat.getDefaultValue();
    }

    public StatInstance(IStat stat, IStatHolder holder, double baseValue) {
        this.stat = Objects.requireNonNull(stat, "Stat cannot be null");
        this.holder = Objects.requireNonNull(holder, "Holder cannot be null");
        this.baseValue = baseValue;
    }

    @Override
    public IStat getStat() {
        return stat;
    }

    @Override
    public double getBase() {
        return baseValue;
    }

    @Override
    public void setBase(double value) {
        if (this.baseValue != value) {
            this.baseValue = value;
            inValidateCache();
        }
    }

    @Override
    public double getFinal() {
        if(!cacheValid){
            recalculate();
        }

        return cacheFinalValue;
    }

    @Override
    public double getFormatValue() {
        double value = getFinal();

        if (stat.getScalingType() == ScalingType.INTEGER){
            return Math.floor(value);
        }
        return value;
    }

    @Override
    public double getTotalFlat() {
        if(!cacheValid){
            recalculate();
        }
        return cacheTotalFlat;
    }

    @Override
    public double getTotalPercent() {
        if(!cacheValid){
            recalculate();
        }
        return cacheTotalPercent;
    }

    @Override
    public boolean addModifier(IStatModifier modifier) {
        if (modifier == null){
            return false;
        }
        if (!modifier.getStatID().equalsIgnoreCase(stat.getID())){
            throw new IllegalArgumentException(
                    "Modifier statId '" + modifier.getStatID() +
                            "' does not match stat '" + stat.getId() + "'"
            );
        }

        IStatModifier previous = modifiers.put(modifier.getID(), modifier);
        inValidateCache();
        return previous == null;
    }

    @Override
    public Optional<IStatModifier> removeModifier(UUID id) {
        if (id == null){
            return Optional.empty();
        }
        IStatModifier removed = modifiers.remove(id);
        if (removed != null){
            inValidateCache();
        }
        return Optional.ofNullable(removed);
    }

    @Override
    public Collection<IStatModifier> removeModifierBySource(String source) {
        if (source == null){
            return List.of();
        }

        List<IStatModifier> removed = modifiers.values().stream()
                .filter(m -> m.getSource().equals(source))
                .toList();

        removed.forEach(m -> modifiers.remove(m.getID()));

        if (!removed.isEmpty()){
            inValidateCache();
        }
        return removed;
    }

    @Override
    public void clearModifiers() {
        if (!modifiers.isEmpty()){
            modifiers.clear();
            inValidateCache();
        }
    }

    @Override
    public int cleanExpiredModifiers() {
        List<UUID> expiredIds = modifiers.values().stream()
                .filter(IStatModifier::isExpired)
                .map(IStatModifier::getID)
                .toList();

        expiredIds.forEach(modifiers::remove);

        if (!expiredIds.isEmpty()){
            inValidateCache();
        }

        return expiredIds.size();
    }
    @Override
    public Optional<IStatModifier> getModifier(UUID id) {
        if (id == null){
            return Optional.empty();
        }


        return Optional.ofNullable(modifiers.get(id));
    }

    @Override
    public Collection<IStatModifier> getModifiersBySource(String source) {
        if (source == null){
            return List.of();
        }

        return modifiers.values().stream().filter(m -> m.getSource().equals(source)).toList();
    }

    @Override
    public Collection<IStatModifier> getModifiersByType(ModifierType type) {
        if (type == null){
            return List.of();
        }

        return modifiers.values().stream().filter(m -> m.getType().equals(type)).toList();
    }

    @Override
    public Collection<IStatModifier> getActiveModifiers() {
        return modifiers.values().stream()
                .filter(m-> !m.isExpired())
                .filter(IStatModifier::isActive)
                .filter(m -> m.meetsCondition(holder))
                .toList();
    }


    @Override
    public Collection<IStatModifier> getAllModifiers() {
        return List.of();
    }

    //미사용
    @Override
    public double getTotalMultiply() {
        return 0;
    }
    @Override
    public void inValidateCache() {
        cacheValid = false;
    }

    @Override
    public int getModifierCount() {
        return modifiers.size();
    }

    /**
     * 스탯 재계산
     * 공식: (Base + Σ FLAT) × (1 + Σ PERCENT)
     */
    private void recalculate(){
        Collection<IStatModifier> activeModifiers = getActiveModifiers();

        // 스택 처리된 Modifier 가져옴
        List<IStatModifier> processedModifiers = processStacks(activeModifiers);

        // FLAT 타입 계산
        cacheTotalFlat = processedModifiers.stream()
                .filter(m -> m.getType() == ModifierType.FLAT)
                .sorted(Comparator.comparingInt(IStatModifier::getPriority))
                .mapToDouble(IStatModifier::getValue).sum();

        //PERCENT 타입 계산
        cacheTotalPercent = processedModifiers.stream()
                .filter(m-> m.getType() == ModifierType.PERCENT)
                .sorted(Comparator.comparingInt(IStatModifier::getPriority))
                .mapToDouble(IStatModifier::getValue).sum();

        double finalValue = (baseValue + cacheTotalFlat) * (1 + cacheTotalPercent);

        finalValue = Math.max(stat.getMaxValue(),finalValue);
        finalValue = Math.min(stat.getMinValue(),finalValue);

        cacheFinalValue = finalValue;
        cacheValid = true;
    }


    /**
     * 스택 처리
     * - stackable=false: 같은 source에서 가장 높은 값만
     * - stackable=true: maxStacks까지만 적용
     */
    private List<IStatModifier> processStacks(Collection<IStatModifier> modifiers){
        // source별로 그룹핑
        Map<String,List<IStatModifier>> bySource = modifiers.stream()
                .collect(Collectors.groupingBy(IStatModifier::getSource));

        List<IStatModifier> result = new ArrayList<>();

        for (Map.Entry<String, List<IStatModifier>> entry : bySource.entrySet()) {
            List<IStatModifier> sourceModifiers = entry.getValue();

            if (sourceModifiers.isEmpty()){
                continue;
            }

            IStatModifier first = sourceModifiers.get(0);

            if (!first.isStackable()){
                sourceModifiers.stream()
                        .max(Comparator.comparingDouble(m -> Math.abs(m.getValue())))
                        .ifPresent(result::add);
            }else{
                int maxStacks = first.getStackCount();

                if (maxStacks > 0 && sourceModifiers.size() > maxStacks){
                    //최대 스택제한 = 값이 높은 순으로 maxStacks갯수만
                    sourceModifiers.stream()
                            .sorted(Comparator.comparingDouble(m -> Math.abs(m.getValue())))
                            .limit(maxStacks)
                            .forEach(result::add);
                }else{ //제한 없음
                    result.addAll(sourceModifiers);
                }
            }
        }

        return result;
    }
}
