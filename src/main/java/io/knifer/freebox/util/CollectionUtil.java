package io.knifer.freebox.util;

import lombok.experimental.UtilityClass;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
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

    public boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
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

    @Nullable
    public <T> T getFirst(Collection<T> collection) {
        Iterator<T> iterator;

        if (isEmpty(collection)) {
            return null;
        }
        iterator = collection.iterator();

        return iterator.hasNext() ? iterator.next() : null;
    }
}
