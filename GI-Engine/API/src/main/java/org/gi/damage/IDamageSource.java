package org.gi.damage;

import org.gi.stat.IStatHolder;

public interface IDamageSource {
    IStatHolder getAttacker();

    DamageType getDamageType();

    /**
     * 기본 데미지 (스킬/무기 데미지)
     */
    double getBaseDamage();

    /**
     * 스킬 계수 (공격력의 몇 %가 추가되는지)
     * 예: 1.5 = 공격력의 150% 추가
     */
    double getSkillScaling();

    /**
     * 소스 식별자 (스킬 ID, 아이템 ID 등)
     * 예: "skill:fireball", "weapon:sword", "environment:fall"
     */
    String getSourceId();

    String getDisplayName();

    boolean canCritical();

    boolean canEvade();

    boolean canBlock();
}
