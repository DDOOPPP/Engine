package org.gi.storage;

import org.gi.stat.IStatModifier;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class PlayerStatData {
    private final UUID playerUUID;
    private final Map<String,Double> baseValues;
    private final Collection<IStatModifier> permanentModifiers;

    public PlayerStatData(UUID playerUUID, Map<String,Double> baseValues, Collection<IStatModifier> permanentModifiers) {
        this.playerUUID = playerUUID;
        this.baseValues = baseValues;
        this.permanentModifiers = permanentModifiers;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public Map<String, Double> getBaseValues() {
        return baseValues;
    }

    public double getBaseValue(String statId) {
        return baseValues.getOrDefault(statId.toLowerCase(), 0.0);
    }

    public Collection<IStatModifier> getPermanentModifiers() {
        return permanentModifiers;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID playerUUID;
        private Map<String,Double> baseValues;
        private Collection<IStatModifier> permanentModifiers;

        public Builder playerUUID(UUID playerUUID) {
            this.playerUUID = playerUUID;
            return this;
        }

        public Builder baseValue(String statId, double value) {
            this.baseValues.put(statId.toLowerCase(), value);
            return this;
        }

        public Builder baseValues(Map<String, Double> values) {
            this.baseValues.putAll(values);
            return this;
        }

        public Builder permanentModifiers(Collection<IStatModifier> modifiers) {
            this.permanentModifiers = modifiers;
            return this;
        }

        public PlayerStatData build() {
            return new PlayerStatData(playerUUID, baseValues, permanentModifiers);
        }
    }
}
