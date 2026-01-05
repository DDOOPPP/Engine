package org.gi.stat;

import org.gi.stat.enums.ScalingType;
import org.gi.stat.enums.StatCategory;

import java.util.List;

public interface IStat {
    /**
     * @return Snake Case 권장
     * */
    String getID();

    /**
     * @return 실제로 보이는 이름
     * */
    String getDisplayName();

    StatCategory getCategory();

    ScalingType getScalingType();

    List<String> getDescriptions();

    /**
     * @return Stat Default Value
     * */
    double getDefaultValue();

    double getMinValue();

    double getMaxValue();

    boolean isPercent();
}
