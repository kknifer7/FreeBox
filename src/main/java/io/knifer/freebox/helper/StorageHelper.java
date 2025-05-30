package io.knifer.freebox.helper;

import io.github.filelize.Filelizer;
import io.knifer.freebox.model.domain.Savable;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.Optional;

/**
 * 存储
 *
 * @author Knifer
 */
@UtilityClass
public class StorageHelper {

    private final Filelizer filelizer = new Filelizer("data");

    public <T> String save(T object) {
        return filelizer.save(object);
    }

    public void delete(Savable savable) {
        filelizer.delete(savable.getId(), savable.getClass());
    }

    public <T> Optional<T> find(String id, Class<T> valueType) {
        return Optional.ofNullable(filelizer.find(id, valueType));
    }

    public <T> Map<String, T> findAll(Class<T> valueType) {
        return filelizer.findAll(valueType);
    }
}
