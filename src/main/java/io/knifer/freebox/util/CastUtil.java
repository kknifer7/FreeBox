package io.knifer.freebox.util;

import lombok.experimental.UtilityClass;

/**
 * 类型转换工具类
 *
 * @author Knifer
 */
@UtilityClass
public class CastUtil {

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object obj) {
        if (obj == null) {
            return null;
        }

        return (T) obj;
    }
}
