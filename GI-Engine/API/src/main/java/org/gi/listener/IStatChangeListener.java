package org.gi.listener;

import org.gi.stat.IStatHolder;

/**
 * 스탯 변경 리스너
 */
@FunctionalInterface
public interface IStatChangeListener {
    /**
     * 스탯 값이 변경되었을 때 호출
     *
     * @param holder 스탯 보유자
     * @param statId 변경된 스탯 ID
     * @param oldValue 이전 값
     * @param newValue 새 값
     */
    void onStatChange(IStatHolder holder, String statId, double oldValue, double newValue);
}
