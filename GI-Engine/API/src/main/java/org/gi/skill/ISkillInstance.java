package org.gi.skill;

import java.util.UUID;

public interface ISkillInstance {
    ISkill getSkill();
    UUID getHolderUUID();

    int getLevel();

    void setLevel(int level);

    boolean levelUp();

    boolean isOnCooldown();
    double getRemainingCooldown();
    void startCooldown();
    void resetCooldown();

    void reduceCooldown(double seconds);

    boolean isCasting();

    double getCastProgress();

    double getRemainingCastTime();

    void startCasting();
    void cancelCasting();
    void completeCasting();

    void tickCasting(double deltaSeconds);

    boolean isToggled();
    void toggle();
    void setToggled(boolean toggled);
}
