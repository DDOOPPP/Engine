package org.gi;

public class Util {
    public static  <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value) {
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return enumClass.getEnumConstants()[0]; // 기본값
        }
    }
}
