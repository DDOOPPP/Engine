package org.gi.resource;

public interface IResourceConfig {
    String getId();

    String getDisplayName();

    String getColor();

    String getIcon();

    String getMaxStatId();

    boolean isRegenEnabled();
    String getRegenStatId();
    double getRegenFixValue();
    double getRegenInterval();

    boolean isChargeEnabled();
    double getChargeOnDealDamage();
    double getChargeOnTakeDamage();
    double getChargeOnKill();
    double getChargeOnCritBonus();

    boolean isDecayEnabled();
    double getDecayDelay();
    double getDecayPerSecond();
    double getDecayMinvalue();

    boolean hasOnCombatStart();
    double getOnCombatStartValue();
}
