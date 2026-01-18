package org.gi.damage;

import org.gi.stat.IStatHolder;

import java.util.concurrent.ThreadLocalRandom;

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
        double baseDamage = source.getBaseDamage();
        // 1. 공격력 체크
        if (attacker != null) {
            String attackStatId = source.getDamageType() == DamageType.MAGICAL
                    ? MAGIC_POWER
                    : ATTACK_POWER;

            double attackerPower = attacker.getStat(attackStatId);
            baseDamage += attackerPower * source.getSkillScaling();
        }

        // 2. 회피 체크
        if (source.canEvade() && attacker != null && target != null) {
            double accuracy = attacker.getStat(ACCURACY);
            double evasion = target.getStat(EVASION);

            if (rollEvasion(accuracy, evasion)) {
                return result
                        .evaded(true)
                        .finalDamage(0)
                        .rawDamage(baseDamage)
                        .resultType(IDamageResult.ResultType.EVADED)
                        .build();
            }
        }

        // 3. 크리티컬
        boolean isCritical = false;
        double damageAfterCritical = baseDamage;

        if (source.canCritical() && attacker != null) {
            double criticalChance = attacker.getStat(CRITICAL_CHANCE);

            if (rollCritical(criticalChance)) {
                isCritical = true;
                double criticalDamage = attacker.getStat(CRITICAL_DAMAGE);
                damageAfterCritical = baseDamage * criticalDamage;
            }
        }

        result.rawDamage(damageAfterCritical);  // 크리티컬 적용 후 저장
        result.critical(isCritical);

        // 4. 방어력 적용
        double damageAfterDefense = damageAfterCritical;
        double mitigatedDamage = 0;

        if (source.getDamageType().isDefensible() && target != null) {
            String defenseStatId = source.getDamageType().getDefenseStatId();
            double defense = target.getStat(defenseStatId);

            double penetration = 0;
            if (attacker != null) {
                String penStatId = source.getDamageType() == DamageType.MAGICAL
                        ? MAGIC_PENETRATION
                        : ARMOR_PENETRATION;
                penetration = attacker.getStat(penStatId);
            }

            damageAfterDefense = calculateMitigation(damageAfterCritical, defense, penetration);
            mitigatedDamage = damageAfterCritical - damageAfterDefense;
        }

        result.mitigatedDamage(mitigatedDamage);

        // 5. 블록 체크
        boolean isBlocked = false;
        double blockedDamage = 0;
        double damageAfterBlock = damageAfterDefense;

        if (source.canBlock() && target != null) {
            double blockChance = target.getStat(BLOCK_CHANCE);

            if (rollBlock(blockChance)) {
                isBlocked = true;
                double blockAmount = target.getStat(BLOCK_AMOUNT);
                blockedDamage = Math.min(damageAfterDefense, blockAmount);
                damageAfterBlock = damageAfterDefense - blockedDamage;
            }
        }

        result.blocked(isBlocked);
        result.blockedDamage(blockedDamage);

        // 6. 최종 데미지
        double finalDamage = Math.max(0, damageAfterBlock);
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
        //관통력 적용
        double effectiveDefense = Math.max(0,defense - penetration);

        //방어력 감소 공식: damage * (1-defense / (defense + 방어력 상수)
        double reduction = effectiveDefense / (effectiveDefense + DEFENSE_CONSTANT);

        //최소 데미지 보장
        reduction = Math.min(reduction, 1 - MIN_DAMAGE_RATIO);

        return damage * (1-reduction);
    }

    @Override
    public boolean rollCritical(double criticalChance) {
        return roll(criticalChance);
    }

    @Override
    public boolean rollEvasion(double accuracy, double evasion) {
        // 회피 공식: evasion - (accuracy - 1)
        // 확률 1.0이면 회피 그대로, 1.2면 회피 - 0.2
        double effectiveEvasion = Math.max(0, evasion - (accuracy - 1));

        return roll(effectiveEvasion);
    }

    @Override
    public boolean rollBlock(double blockChance) {
        return roll(blockChance);
    }

    private boolean roll(double chance){
        return ThreadLocalRandom.current().nextDouble() < chance;
    }
}
