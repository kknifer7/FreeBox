package io.knifer.freebox.util;

import lombok.experimental.UtilityClass;

import java.util.Collection;

/**
 * 集合工具类
 *
 * @author Knifer
 */
@UtilityClass
public class CollectionUtil {

    public boolean isEmpty(Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public boolean isNotEmpty(Collection<?> collection) {
        return !isEmpty(collection);
    }
}
