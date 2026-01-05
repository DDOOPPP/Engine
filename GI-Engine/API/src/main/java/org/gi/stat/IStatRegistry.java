package org.gi.stat;

import org.gi.stat.enums.ScalingType;
import org.gi.stat.enums.StatCategory;

import java.util.Collection;
import java.util.Optional;

/**
 * 스탯 레지스트리 인터페이스
 *
 * 모든 스탯 정의를 등록하고 관리하는 중앙 저장소.
 * 런타임에 동적으로 스탯을 등록/해제할 수 있음.
 */
public interface IStatRegistry {
    /**
     *
     * @param stat 등록할 스탯
     * @return 스탯 등록 성공 여부
     */
    boolean register(IStat stat);

    /**
     * @param statId 스탯의 고유Id
     * @return 삭제한 스탯
     * */
    Optional<IStat> unregister(String statId);
    /**
     * @param statId 스탯의 고유Id
     * @return 해당 스탯을 반환
     * */
    Optional<IStat> get(String statId);

    /**
     * @return 등록된 전체 스탯
     * */
    Collection<IStat> getAll();

    /**
     * @param statId 조회할 ID
     * @return 등록 여부
     * */
    boolean contains(String statId);

    /**
     * @param category
     * @return 카테고리별 스탯 목록
     * */
    Collection<IStat> getStatsByCategory(StatCategory category);

    /**
     * @param scalingType
     * @return 스케일링 타입별 목록 반환
     * */
    Collection<IStat> getStatsByScalingType(ScalingType scalingType);

    /**
     * @return 등록된 스탯의 갯수
     * */
    int getStatCount();

    /**
     * 메모리 초기화
     * */
    void clear();
    /**
     * 레지스트리 잠금 -> 서버 실행 후 변경 방지
     * */
    void lock();
    /**
     * @return 레지스트리 잠금 여부
     * */
    boolean isLocked();
    /**
     * 서버 종료하지않고 간단한 수정시 잠금 해제 기능
     * */
    void unlock();
}
