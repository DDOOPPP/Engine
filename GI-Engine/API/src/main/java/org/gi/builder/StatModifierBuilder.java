package org.gi.builder;

import org.gi.stat.IStatHolder;
import org.gi.stat.IStatModifier;
import org.gi.stat.enums.ModifierType;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * StatModifier 객체 생성을 위한 빌더
 *
 * Fluent API로 가독성 높은 수정자 생성.
 */
public class StatModifierBuilder {
    private UUID uniqueId;
    private String source;
    private String displayName = "";
    private String statId;
    private ModifierType type = ModifierType.FLAT;
    private double value = 0;
    private int priority = 0;
    private boolean active = true;
    private long expireTime = -1;
    private Predicate<IStatHolder> condition = null;
    private boolean stackable = true;
    private int maxStacks = -1;

    /**
     * 새 빌더 인스턴스 생성
     */
    public static StatModifierBuilder create() {
        return new StatModifierBuilder();
    }

    /**
     * 고유 식별자 (미설정 시 자동 생성)
     */
    public StatModifierBuilder uniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
        return this;
    }

    /**
     * 소스 식별자 (필수)
     * 예: "equipment:helmet", "buff:strength_potion"
     */
    public StatModifierBuilder source(String source) {
        this.source = source;
        return this;
    }

    /**
     * 표시 이름
     */
    public StatModifierBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * 대상 스탯 ID (필수)
     */
    public StatModifierBuilder statId(String statId) {
        this.statId = statId;
        return this;
    }

    /**
     * 연산 타입
     */
    public StatModifierBuilder type(ModifierType type) {
        this.type = type;
        return this;
    }

    /**
     * FLAT 타입으로 설정 + 값
     */
    public StatModifierBuilder flat(double value) {
        this.type = ModifierType.FLAT;
        this.value = value;
        return this;
    }

    /**
     * PERCENT 타입으로 설정 + 값
     *
     * @param value 백분율 (0.1 = 10%)
     */
    public StatModifierBuilder percent(double value) {
        this.type = ModifierType.PERCENT;
        this.value = value;
        return this;
    }

    /**
     * 수정 값
     */
    public StatModifierBuilder value(double value) {
        this.value = value;
        return this;
    }

    /**
     * 우선순위 (낮을수록 먼저 적용)
     */
    public StatModifierBuilder priority(int priority) {
        this.priority = priority;
        return this;
    }

    /**
     * 활성 상태
     */
    public StatModifierBuilder active(boolean active) {
        this.active = active;
        return this;
    }

    /**
     * 만료 시간 (밀리초 타임스탬프)
     */
    public StatModifierBuilder expireTime(long expireTime) {
        this.expireTime = expireTime;
        return this;
    }

    /**
     * 지속 시간 설정 (현재 시간 기준)
     *
     * @param durationMillis 지속 시간 (밀리초)
     */
    public StatModifierBuilder duration(long durationMillis) {
        this.expireTime = System.currentTimeMillis() + durationMillis;
        return this;
    }

    /**
     * 지속 시간 설정 (초 단위)
     *
     * @param seconds 지속 시간 (초)
     */
    public StatModifierBuilder durationSeconds(int seconds) {
        return duration(seconds * 1000L);
    }

    /**
     * 영구 지속 (만료 없음)
     */
    public StatModifierBuilder permanent() {
        this.expireTime = -1;
        return this;
    }

    /**
     * 조건부 활성화
     */
    public StatModifierBuilder condition(Predicate<IStatHolder> condition) {
        this.condition = condition;
        return this;
    }

    /**
     * 스택 가능 여부
     */
    public StatModifierBuilder stackable(boolean stackable) {
        this.stackable = stackable;
        return this;
    }

    /**
     * 최대 스택 수
     */
    public StatModifierBuilder maxStacks(int maxStacks) {
        this.maxStacks = maxStacks;
        return this;
    }

    public IStatModifier build() {
        validate();

        if (uniqueId == null){
            uniqueId = UUID.randomUUID();
        }

        return new IStatModifierImpl(
                uniqueId,
                source,
                displayName,
                statId,
                type,
                value,
                priority,
                active,
                expireTime,
                condition,
                stackable,
                maxStacks
        );
    }

    private void validate() {
        if (source == null || source.isBlank()) {
            throw new IllegalStateException("Modifier source is required");
        }
        if (statId == null || statId.isBlank()) {
            throw new IllegalStateException("Modifier statId is required");
        }
        if (type == null) {
            throw new IllegalStateException("Modifier type is required");
        }
    }
    /**
     * 불변 StatModifier 구현체 (내부 클래스)
     */
    private record IStatModifierImpl(
            UUID uniqueId,
            String source,
            String displayName,
            String statId,
            ModifierType type,
            double value,
            int priority,
            boolean active,
            long expireTime,
            Predicate<IStatHolder> condition,
            boolean stackable,
            int maxStacks
    ) implements IStatModifier {

        @Override
        public UUID getID() {
            return uniqueId;
        }

        @Override
        public String getSource() {
            return source;
        }

        @Override
        public String getStatID() {
            return statId;
        }

        @Override
        public ModifierType getType() {
            return type;
        }

        @Override
        public double getValue() {
            return value;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public long getExpireTime() {
            return expireTime;
        }

        @Override
        public Predicate<IStatHolder> getCondition() {
            return condition;
        }

        @Override
        public boolean isStackable() {
            return stackable;
        }

        @Override
        public int getStackCount() {
            return maxStacks;
        }
    }
}
