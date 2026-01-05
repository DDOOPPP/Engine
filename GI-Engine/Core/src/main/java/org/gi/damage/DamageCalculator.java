package org.gi.damage;

import org.gi.stat.IStatHolder;

public class DamageCalculator implements IDamageCalculator {

    /**
     * 방어력 계산 상수
     * 높을수록 효율 감소
     * */
    private static final double DEFENSE_CONSTANT = 1000;

    /**
     * 최소 데미지 비율 (방어력으로 이 이상은 감소 불가)
     */
    private static final double MIN_DAMAGE_RATIO = 0.1;

    private static final String ATTACK_POWER = "attack_power";
    private static final String MAGIC_POWER = "magic_power";
    private static final String CRITICAL_CHANCE = "critical_chance";
    private static final String CRITICAL_DAMAGE = "critical_damage";
    private static final String ACCURACY = "accuracy";
    private static final String ARMOR_PENETRATION = "armor_penetration";
    private static final String MAGIC_PENETRATION = "magic_penetration";
    private static final String EVASION = "evasion";
    private static final String BLOCK_CHANCE = "block_chance";
    private static final String BLOCK_AMOUNT = "block_amount";

    @Override
    public IDamageResult calculate(IDamageSource source, IStatHolder target) {
        DamageResult.Builder result = DamageResult.builder().source(source).target(target);

        IStatHolder attacker = source.getAttacker();

        // 회피 체크
        if (source.canEvade() && attacker != null) {
            double accuracy = attacker.getStat(ACCURACY);
            double evasion = target.getStat(EVASION);

            if (rollEvasion(accuracy, evasion)) {
                return result
                        .evaded(true)
                        .finalDamage(0)
                        .rawDamage(0)
                        .resultType(IDamageResult.ResultType.EVADED)
                        .build();
            }
        }

        double rawDamage = source.getBaseDamage();

        if (attacker != null){
            String attackStatId = source.getDamageType() == DamageType.MAGICAL
                    ? MAGIC_POWER
                    : ATTACK_POWER;

            double attackerPower = attacker.getStat(attackStatId);
            rawDamage += attackerPower * source.getSkillScaling();
        }
        result.rawDamage(rawDamage);

        boolean isCritical = false;

        if (source.canCritical() && attacker != null) {
            double criticalChance = attacker.getStat(CRITICAL_CHANCE);

            if (rollCritical(criticalChance)) {
                isCritical = true;
                double criticalDamage = attacker.getStat(CRITICAL_DAMAGE);
                rawDamage *= criticalDamage;
            }
        }

        result.critical(isCritical);

// ========== 4. 방어력 적용 ==========
        double defense = 0;
        double penetration = 0;
        double mitigatedDamage = 0;

        if (source.getDamageType().isDefensible()) {
            String defenseStatId = source.getDamageType().getDefenseStatId();
            defense = target.getStat(defenseStatId);

            if (attacker != null) {
                String penStatId = source.getDamageType() == DamageType.MAGICAL
                        ? MAGIC_PENETRATION
                        : ARMOR_PENETRATION;
                penetration = attacker.getStat(penStatId);
            }

            double beforeMitigation = rawDamage;
            rawDamage = calculateMitigation(rawDamage, defense, penetration);
            mitigatedDamage = beforeMitigation - rawDamage;
        }

        result.mitigatedDamage(mitigatedDamage);

        // ========== 5. 블록 체크 ==========
        boolean isBlocked = false;
        double blockedDamage = 0;

        if (source.canBlock()) {
            double blockChance = target.getStat(BLOCK_CHANCE);

            if (rollBlock(blockChance)) {
                isBlocked = true;
                double blockAmount = target.getStat(BLOCK_AMOUNT);
                blockedDamage = Math.min(rawDamage, blockAmount);
                rawDamage -= blockedDamage;
            }
        }

        result.blocked(isBlocked);
        result.blockedDamage(blockedDamage);

        // ========== 6. 최종 데미지 ==========
        double finalDamage = Math.max(0, rawDamage);
        result.finalDamage(finalDamage);

        // 결과 타입 결정
        IDamageResult.ResultType resultType;
        if (isBlocked && finalDamage <= 0) {
            resultType = IDamageResult.ResultType.FULL_BLOCKED;
        } else if (isBlocked) {
            resultType = IDamageResult.ResultType.BLOCKED;
        } else if (isCritical) {
            resultType = IDamageResult.ResultType.CRITICAL;
        } else {
            resultType = IDamageResult.ResultType.HIT;
        }

        result.resultType(resultType);

        return result.build();
    }

    @Override
    public double calculateMitigation(double damage, double defense, double penetration) {
        return 0;
    }

    @Override
    public boolean rollCritical(double criticalChance) {
        return false;
    }

    @Override
    public boolean rollEvasion(double accuracy, double evasion) {
        return false;
    }

    @Override
    public boolean rollBlock(double blockChance) {
        return false;
    }
}
