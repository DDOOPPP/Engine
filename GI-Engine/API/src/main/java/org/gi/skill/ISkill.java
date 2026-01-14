package org.gi.skill;

import java.util.List;

public interface ISkill {
    String getId();
    String getDisplayName();

    List<String> getDescription();

    SkillType getType();

    CastingType getCastingType();

    String getRequiredClass();

    int getLevel();

    boolean isLevelable();

    String getResourceId();

    double getResourceCost(int level);
    double getCooldown(int level);
    double getCastingTime(int level);

    boolean isInterruptible();

    TargetingType getTargetingType();
    double getRange();
    double getRadius();
    double getProjectileSpeed();

    boolean isPiercing();

    double getConeAngle();
    double getLineWidth();

    //List<ISkillEffect> getEffects();
}
