package io.knifer.freebox.util;

import lombok.experimental.UtilityClass;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Predicate;

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

    @Nullable
    public <T> T findFirst(Collection<T> collection, Predicate<T> predicate) {
        Iterator<T> iterator;
        T t;

        if (isEmpty(collection)) {
            return null;
        }
        iterator = collection.iterator();
        while (iterator.hasNext()) {
            t = iterator.next();

            if (predicate.test(t)) {
                return t;
            }
        }

        return null;
    }
}
