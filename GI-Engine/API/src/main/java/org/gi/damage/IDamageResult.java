package org.gi.damage;

import org.gi.stat.IStatHolder;

public interface IDamageResult {
    IDamageSource getSource();

    /**
     * 타겟 대상
     */
    IStatHolder getTarget();

    /**
     * 최종 데미지
     */
    double getFinalDamage();

    /**
     * 원본 데미지 (감소 전)
     */
    double getRawDamage();

    /**
     * 방어력으로 감소된 데미지
     */
    double getMitigationDamage();

    boolean isCritical();

    boolean isEvade();

    boolean isBlocked();

    double getBlockDamage();

    ResultType getResultType();

    enum ResultType {
        /** 정상 적중 */
        HIT,
        /** 크리티컬 적중 */
        CRITICAL,
        /** 회피됨 */
        EVADED,
        /** 블록됨 (일부 데미지) */
        BLOCKED,
        /** 완전 블록 (데미지 0으로 할 생각) */
        FULL_BLOCKED
    }

}
