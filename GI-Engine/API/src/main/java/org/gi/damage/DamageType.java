package org.gi.damage;

public enum DamageType {
    PHYSICAL ("물리","armor"),
    MAGICAL ("마법","magic_resistance"),
    TRUE("고정",null);

    private final String displayName;
    private final String defenseStatId;

    DamageType(String displayName, String defenseStatId) {
        this.displayName = displayName;
        this.defenseStatId = defenseStatId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefenseStatId() {
        return defenseStatId;
    }

    public boolean isDefensible() {
        return defenseStatId != null;
    }
}
