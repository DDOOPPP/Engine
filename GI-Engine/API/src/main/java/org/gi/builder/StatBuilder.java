package org.gi.builder;

import org.gi.stat.enums.ScalingType;
import org.gi.stat.enums.StatCategory;
import org.gi.stat.IStat;

import java.util.List;

/**
 * Stat 객체 생성을 위한 빌더
 * Fluent API로 가독성 높은 스탯 정의 생성
 */
public class StatBuilder {
    String id;
    String displayName;
    List<String> descriptions;
    StatCategory category;
    ScalingType scalingType;
    double defaultValue = 0;
    double minValue = Double.MIN_VALUE;
    double maxValue = Double.MAX_VALUE;
    boolean percentageBased  = false;

    public static StatBuilder create() {
        return new StatBuilder();
    }

    /**
     * 스탯 고유 식별자 (필수)
     */
    public StatBuilder id(String id) {
        this.id = id;
        return this;
    }

    /**
     * 표시 이름 (필수)
     */
    public StatBuilder displayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    /**
     * 스탯 설명
     */
    public StatBuilder description(List<String> descriptions) {
        this.descriptions = descriptions;
        return this;
    }

    /**
     * 스탯 카테고리
     */
    public StatBuilder category(StatCategory category) {
        this.category = category;
        return this;
    }

    /**
     * 스케일링 타입
     */
    public StatBuilder scalingType(ScalingType scalingType) {
        this.scalingType = scalingType;
        return this;
    }

    /**
     * 기본값
     */
    public StatBuilder defaultValue(double defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    /**
     * 최소값
     */
    public StatBuilder minValue(double minValue) {
        this.minValue = minValue;
        return this;
    }

    /**
     * 최대값
     */
    public StatBuilder maxValue(double maxValue) {
        this.maxValue = maxValue;
        return this;
    }

    /**
     * 최소/최대값 한번에 설정
     */
    public StatBuilder range(double min, double max) {
        this.minValue = min;
        this.maxValue = max;
        return this;
    }

    /**
     * 퍼센트 기반 여부
     */
    public StatBuilder percentageBased(boolean percentageBased) {
        this.percentageBased = percentageBased;
        return this;
    }

    /**
     * Stat 객체 생성
     *
     * @return 불변 Stat 객체
     * @throws IllegalStateException 필수 필드 누락 시
     */
    public IStat build() {
        validate();
        range(minValue,maxValue);
        return new IStatImpl(
                id,
                displayName,
                descriptions,
                category,
                scalingType,
                defaultValue,
                minValue,
                maxValue,
                percentageBased
        );
    }

    private void validate() {
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("Stat id is required");
        }
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalStateException("Stat displayName is required");
        }
        if (minValue > maxValue) {
            throw new IllegalStateException("minValue cannot be greater than maxValue");
        }
        if (defaultValue < minValue || defaultValue > maxValue) {
            throw new IllegalStateException("defaultValue must be within min/max range");
        }
    }

    /**
     * 불변 Stat 구현체 (내부 클래스)
     */
    private record IStatImpl(
            String id,
            String displayName,
            List<String> description,
            StatCategory category,
            ScalingType scalingType,
            double defaultValue,
            double minValue,
            double maxValue,
            boolean percentageBased
    ) implements IStat {

        @Override
        public String getID() {
            return id;
        }

        @Override
        public String getDisplayName() { return displayName; }

        @Override
        public StatCategory getCategory() { return category; }

        @Override
        public ScalingType getScalingType() { return scalingType; }

        @Override
        public List<String> getDescriptions() {
            return description;
        }

        @Override
        public double getDefaultValue() { return defaultValue; }

        @Override
        public double getMinValue() { return minValue; }

        @Override
        public double getMaxValue() { return maxValue; }

        @Override
        public boolean isPercent() {
            return percentageBased;
        }
    }
}

