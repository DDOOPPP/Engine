package org.gi.resource;

public interface IResource {
    IResourceConfig getConfig();

    double getCurrent();
    double getMax();

    void setCurrent(double current);
    boolean consume(double amount);
    void restore(double amount);

    void charge(double amount);
    void fill();

    void empty();

    default double getPercent() {
        return getMax() > 0 ? getCurrent() / getMax() : 0;
    }

    default boolean hasEnough(double amount) {
        return getCurrent() >= amount;
    }

    void tickRegen(double deltaSeconds);

    void tickDecay(double deltaSeconds);

    void onCombatStartChange(boolean isCombat);
}
