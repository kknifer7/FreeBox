package io.knifer.freebox.helper;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.knifer.freebox.util.CastUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

/**
 * 缓存
 *
 * @author Knifer
 */
@Slf4j
@UtilityClass
public class CacheHelper {

    private static final Cache<String, Object> CACHE = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .build();

    public void put(String key, Object value) {
        CACHE.put(key, value);
    }

    @Nullable
    public <T> T get(String key) {
        try {
            return CastUtil.cast(CACHE.getIfPresent(key));
        } catch (ClassCastException e) {
            log.warn("Cache value type error", e);

            return null;
        }
    }

    public void del(String key) {
        CACHE.invalidate(key);
    }
}
