package io.knifer.freebox.util;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;
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

    public <T> Optional<T> findFirst(Collection<T> collection, Predicate<T> predicate) {
        Iterator<T> iterator;
        T t;

        if (isEmpty(collection)) {
            return Optional.empty();
        }
        iterator = collection.iterator();
        while (iterator.hasNext()) {
            t = iterator.next();

            if (predicate.test(t)) {
                return Optional.of(t);
            }
        }

        return Optional.empty();
    }
}
