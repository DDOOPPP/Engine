package org.gi.gIEngine.model;

import org.bukkit.entity.Player;
import org.gi.stat.IStatInstance;
import org.gi.stat.IStatRegistry;
import org.gi.stat.StatHolder;
import org.gi.stat.enums.HolderType;

import java.util.UUID;

public class PlayerStatHolder extends StatHolder {
    private final UUID playerId;
    private final String playerName;

    public PlayerStatHolder(IStatRegistry statRegistry, UUID playerId, String playerName) {
        super(statRegistry);
        this.playerId = playerId;
        this.playerName = playerName;
    }

    public PlayerStatHolder(IStatRegistry statRegistry, Player player) {
        this(statRegistry, player.getUniqueId(), player.getName());
    }

    @Override
    public UUID getUUID() {
        return playerId;
    }

    @Override
    public HolderType getType() {
        return HolderType.PLAYER;
    }

    @Override
    public String getName() {
        return playerName;
    }

    @Override
    public void setBase(String statId, double value) {
        getOrCreateIStatInstance(statId).setBase(value);
    }

    @Override
    public void clearAllModifiers() {
        for (IStatInstance instance : getAllIStatInstances()) {
            instance.clearModifiers();
        }
    }
}
