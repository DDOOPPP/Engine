package org.gi.stat;

import org.gi.stat.enums.ModifierType;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * 스탯 인스턴스 인터페이스
 *
 * 특정 StatHolder가 보유한 단일 스탯의 실제 인스턴스.
 * Modifier들을 관리하고 최종 값을 계산함.
 */
public interface IStatInstance {

    /**
     * 해당 인스턴스의 스탯 정의
     @return Stat 객체
     */
    IStat getStat();

    double getBase();

    void setBase(double base);

    double getFinal();

    /**
     @return 해당 ScalingType에 맞게
     INTEGER -> 정수
     DECIMAL -> 소수점 유지
     */
    double getFormatValue();

    /**
     * 수정자 추가
     @param modifier 추가할 수정자
     @return 추가 여부
     */
    boolean addModifier(IStatModifier modifier);

    /**
     @param id 제거할 수정자의 UUID
     @return 제거된 수정자 없으면 Empty
     */
    Optional<IStatModifier> removeModifier(UUID id);

    /**
     @param source 식별자
     @return source로 제거된 수정자
     */
    Collection<IStatModifier> removeModifierBySource(String source);

    /**
     @return 모든 수정자 제거
     */
    void clearModifiers();

    /**
     *
     * @return 만료된 수정자 제거
     */
    int cleanExpiredModifiers();

    /**
     @return 모든 수정자 조회
     */
    Collection<IStatModifier> getAllModifiers();

    /**
     @return 활성화된 수정자 조회
     */
    Collection<IStatModifier> getActiveModifiers();

    Collection<IStatModifier> getModifiersBySource(String source);

    Collection<IStatModifier> getModifiersByType(ModifierType type);

    /**
     @param id 제거할 수정자의 UUID
     @return 특정 수정자 조회 없으면 Empty
     */
    Optional<IStatModifier> getModifier(UUID id);

    double getTotalFlat();

    double getTotalPercent();

    double getTotalMultiply();

    /**
     * 캐싱 값 무효
     * 수정자 변경 시 자동 호출로 사용, 강제 재계산 필요시
     */
    void inValidateCache();

    /**
     @return 등록된 수정자 수
     */
    int getModifierCount();
}
