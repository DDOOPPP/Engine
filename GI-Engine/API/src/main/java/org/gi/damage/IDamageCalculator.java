package org.gi.damage;

import org.gi.stat.IStatHolder;

public interface IDamageCalculator {

    /**
     * @param source 데미지 원인
     * @param target 공격 대상
     * @return 계산 결과
     * */
    IDamageResult calculate(IDamageSource source, IStatHolder target);

    /**
     * @param damage 원래 데미지
     * @param defense 방어력
     * @param penetration 관통
     * @return 감소된 값
     * */
    double calculateMitigation(double damage, double defense, double penetration);

    /**
     * @param criticalChance 원래 데미지
     * @return 크리티컬 발생 여부
     * */
    boolean rollCritical(double criticalChance);

    /**
     * @param accuracy 확률
     * @param evasion 회피력
     * @return 회피 여부
     * */
    boolean rollEvasion(double accuracy, double evasion);
    /**
     * @param blockChance 막기 확률
     * @return 막기 여부
     * */
    boolean rollBlock(double blockChance);

}
