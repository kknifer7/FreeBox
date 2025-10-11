package io.knifer.freebox.helper;

import cn.hutool.core.io.FileUtil;
import io.github.filelize.Filelizer;
import io.knifer.freebox.exception.FBException;
import io.knifer.freebox.model.domain.Savable;
import lombok.experimental.UtilityClass;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 存储
 *
 * @author Knifer
 */
@UtilityClass
public class StorageHelper {

    private final Path LOCAL_STORAGE_PATH;
    private final Path TEMP_STORAGE_PATH;
    private final Path LIVE_CONFIG_CACHE_PATH;
    private final Path SPIDER_CACHE_PATH;
    private final Filelizer filelizer;

    static {
        switch (SystemHelper.getPlatform()) {
            case WINDOWS:
                LOCAL_STORAGE_PATH = Path.of(
                        System.getProperty("user.home"), "AppData", "Local", "FreeBox"
                );
                TEMP_STORAGE_PATH = LOCAL_STORAGE_PATH.resolve("temp");
                break;
            case DEB_LINUX:
            case RPM_LINUX:
            case OTHER_LINUX:
            case MAC:
                LOCAL_STORAGE_PATH = Path.of(System.getProperty("user.home"), ".freebox");
                TEMP_STORAGE_PATH = Path.of("/tmp/freebox/");
                break;
            default:
                throw new FBException("unsupported platform");
        }
        LIVE_CONFIG_CACHE_PATH = LOCAL_STORAGE_PATH.resolve(Path.of("data", "live_cache"));
        SPIDER_CACHE_PATH = LOCAL_STORAGE_PATH.resolve(Path.of("data", "spider_cache"));
        filelizer = new Filelizer(
                LOCAL_STORAGE_PATH.resolve("data").toString()
        );
    }

    public void clearData() {
        FileUtil.del(LOCAL_STORAGE_PATH);
    }

    public void clearTemp() {
        FileUtil.clean(TEMP_STORAGE_PATH.toString());
    }

    public Path getLocalStoragePath() {
        return LOCAL_STORAGE_PATH;
    }

    public Path getTempStoragePath() {
        return TEMP_STORAGE_PATH;
    }

    public Path getLiveConfigCachePath() {
        return LIVE_CONFIG_CACHE_PATH;
    }

    public Path getSpiderCachePath() {
        return SPIDER_CACHE_PATH;
    }

    public <T> String save(T object) {
        return filelizer.save(object);
    }

    public <T> List<String> saveAll(List<T> objects) {
        return filelizer.saveAll(objects);
    }

    public void delete(Savable savable) {
        filelizer.delete(savable.getId(), savable.getClass());
    }

    public void delete(String id, Class<?> valueType) {
        filelizer.delete(id, valueType);
    }

    public <T> Optional<T> find(String id, Class<T> valueType) {
        return Optional.ofNullable(filelizer.find(id, valueType));
    }

    public <T> Map<String, T> findAll(Class<T> valueType) {
        return filelizer.findAll(valueType);
    }
}
