package org.gi.stat;

import org.gi.stat.enums.ModifierType;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * 실제 스탯 수장자 인터페이스
 * 목적
 * - 착용 장비 및 버프, 패시브, 스킬로 인한 스탯을 수정할때
 * - StatInstance에 등록되어 사용될 예정?
 * */
public interface IStatModifier {
    /**
     @return 스탯 고유 ID
     */
    UUID getID();

    /**
     @return 스탯이 어디서 영향을 받았는지
     */
    String getSource();

    /**
     @return 어떤 스탯이 영향을 받을껀지
     */
    String getStatID();

    /**
     @return 실제 스탯을 계산할 연산자
     FLAT -> +
     PERCENT -> %계산
     MULTIPLY -> 곱연산

     차후 Debuff시 Minus 고려중
     */
    ModifierType getType();

    /**
     @return 실제 스탯을 계산할 연산자
     FLAT -> +
     PERCENT -> %계산
     MULTIPLY -> 곱연산

     차후 Debuff시 Minus 고려중
     */
    double getValue();

    /**
     @return 계산 우선 순위
     */
    int getPriority();

    /**
     @return 특정 상황에서 사용될 조건부 활성화
        ex) HP가 30% 이하 이거나 MP가 10% 이하 인경우
     */
    boolean isActive();

    /**
     @return Buff , Debuff의 만료시간
     */
    long getExpireTime();


    /**
     * 조건부 활성화 조건
     * StatHolder의 상태에 따라 동적으로 활성/비활성
     * 예: HP 30% 이하일 때만 적용되는 버프
     *
     * @return 조건 Predicate, null이면 항상 활성
     */
    Predicate<IStatHolder> getCondition();

    /**
     * 조건 충족 여부 확인
     *
     * @param holder 스탯 보유자
     * @return 조건을 충족하면 true
     */
    default boolean meetsCondition(IStatHolder holder) {
        Predicate<IStatHolder> condition = getCondition();
        return condition == null || condition.test(holder);
    }

    /**
     @return 만료 되었는지의 여부 만료되면 True
     */
    default boolean isExpired() {
        long expireTime = getExpireTime();
        return expireTime > 0 && expireTime <= System.currentTimeMillis();
    }

    /**
     @return 중첩 스탯인지?
     */
    boolean isStackable();

    /**
     @return isStackable()이 True인 경우
     -1이면 무제한 중첩
     */
    int getStackCount();
}
