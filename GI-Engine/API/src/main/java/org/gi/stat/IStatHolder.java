package org.gi.stat;

import org.gi.stat.enums.HolderType;
import org.gi.listener.IStatChangeListener;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * 스탯 보유자 인터페이스
 *
 * 스탯을 가질 수 있는 모든 엔티티의 추상화.
 * 플레이어, 몬스터, NPC, 심지어 아이템도 될 수 있음.
 */
public interface IStatHolder {
    /**
     * 보유자 고유 식별자
     * 플레이어면 UUID, 몬스터면 엔티티 UUID 등
     *
     * @return 고유 식별자
     */
    UUID getUUID();

    /**
     * 보유자 타입
     *
     * @return 보유자의 타입
     */
    HolderType getType();

    /**
     * 보유자 이름
     *
     * @return 보유자의 이름
     */
    String getName();

    /**
     * 특정 스탯 인스턴스 조회
     *
     * @param statId 스탯 ID
     * @return 스탯 인스턴스, 없으면 empty
     */
    Optional<IStatInstance> getIStatInstance(String statId);

    /**
     * 특정 스탯 인스턴스 조회
     *
     * @param statId 스탯 ID
     * @return 스탯 인스턴스, 없으면 생성
     * @throws IllegalArgumentException 등록되지 않은 StatId 인경우
     */
    IStatInstance getOrCreateIStatInstance(String statId);

    /**
     * 보유한 스탯 목록
     *
     * @return 보유한 스탯 인스턴스 목록 없으면 Empty
     */
    Collection<IStatInstance> getAllIStatInstances();

    /**
     * 특정 스탯 인스턴스 존재여부
     *
     * @param statId 스탯 ID
     * @return 있으면 True
     */
    boolean hasIStatInstance(String statId);

    default double getStat(String statId){
        return getIStatInstance(statId).map(IStatInstance::getFinal).orElse(0.0);
    }

    default double getBase(String statId){
        return getIStatInstance(statId).map(IStatInstance::getBase).orElse(0.0);
    }

    default void setBase(String statId, double value){
        getOrCreateIStatInstance(statId).setBase(value);
    }

    default boolean addModifier(IStatModifier modifier){
        return getOrCreateIStatInstance(modifier.getStatID()).addModifier(modifier);
    }

    default Optional<IStatModifier> removeModifier(String statId, UUID ModifierId){
        return getIStatInstance(statId).flatMap(instance -> instance.removeModifier(ModifierId));
    }

    /**
     * @param source 소스 식별자
     * @return 제거된 수정자 수
     * */
    int removeAllModifiersBySource(String source);

    /**
     * @return 모든 스탯의 만료된 수정자 제거 수량
     * */
    int cleanAllExpiredModifiers();


    /**
     * 모든 수정자 제거
     * */
    void clearAllModifiers();

    /**
     * 모든 스탯 재계산
     * 캐싱 무효 및 강제 재계산
     * */
    void recalculateAllStat();

    /**
     * 스탯 변경 리스너 등록
     *
     * @param listener 스탯 변경 시 호출될 리스너
     */
    void addStatChangeListener(IStatChangeListener listener);

    /**
     * 스탯 변경 리스너 제거
     *
     * @param listener 제거할 리스너
     */
    void removeStatChangeListener(IStatChangeListener listener);
}
