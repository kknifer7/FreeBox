package io.knifer.freebox.constant;

import java.util.Optional;

public interface ValueEnum<V> {

    V getValue();

    static <V, E extends ValueEnum<V>> Optional<E> valueOf(Class<E> enumClass, V value) {
        for (E each : enumClass.getEnumConstants()) {
            if (each.getValue().equals(value)) {

                return Optional.of(each);
            }
        }

        return Optional.empty();
    }

    static boolean isValid(Class<? extends ValueEnum<?>> enumClass, Object value) {
        for (ValueEnum<?> each : enumClass.getEnumConstants()) {
            if (each.getValue().equals(value)) {

                return true;
            }
        }

        return false;
    }
}
