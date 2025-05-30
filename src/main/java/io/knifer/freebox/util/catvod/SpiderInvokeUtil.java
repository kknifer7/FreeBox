package io.knifer.freebox.util.catvod;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ToastHelper;
import javafx.application.Platform;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 爬虫执行工具类
 *
 * @author Knifer
 */
@Slf4j
@UtilityClass
public class SpiderInvokeUtil {

    public void init(Object spider, @Nullable String extend) {
        if (extend == null) {
            getMethod(spider, "init").ifPresent(method -> invoke(spider, method));
        } else {
            getMethod(spider, "init", String.class)
                    .ifPresent(method -> invoke(spider, method, extend));
        }
    }

    @Nullable
    public String homeContent(Object spider, boolean filter) {
        return getMethod(spider, "homeContent", boolean.class)
                .map(method -> SpiderInvokeUtil.<String>invoke(spider, method, filter))
                .orElse(null);
    }

    @Nullable
    public String homeVideoContent(Object spider) {
        return getMethod(spider, "homeVideoContent")
                .map(method -> SpiderInvokeUtil.<String>invoke(spider, method))
                .orElse(null);
    }

    @Nullable
    public String categoryContent(
            Object spider, String tid, String pg, boolean filter, @Nullable HashMap<String, String> extend
    ) {
        return getMethod(spider, "categoryContent", String.class, String.class, boolean.class, HashMap.class)
                .map(method -> SpiderInvokeUtil.<String>invoke(spider, method, tid, pg, filter, extend))
                .orElse(null);
    }

    @Nullable
    public String detailContent(Object spider, List<String> ids) {
        return getMethod(spider, "detailContent", List.class)
                .map(method -> SpiderInvokeUtil.<String>invoke(spider, method, ids))
                .orElse(null);
    }

    @Nullable
    public String searchContent(Object spider, String key, boolean quick) {
        Class<?> clazz = spider.getClass();
        Method method;
        Object result = null;

        try {
            method = clazz.getMethod("searchContent", String.class, boolean.class);
            result = method.invoke(spider, key, quick);
        } catch (Throwable ignored) {}

        return result == null ? null : (String) result;
    }

    @Nullable
    public String searchContent(Object spider, String key, boolean quick, String pg) {
        Class<?> clazz = spider.getClass();
        Method method;
        Object result = null;

        try {
            method = clazz.getMethod("searchContent", String.class, boolean.class, String.class);
            result = method.invoke(spider, key, quick, pg);
        } catch (Throwable ignored) {}

        return result == null ? null : (String) result;
    }

    @Nullable
    public String playerContent(Object spider, String flag, String id, List<String> vipFlags) {
        return getMethod(spider, "playerContent", String.class, String.class, List.class)
                .map(method -> SpiderInvokeUtil.<String>invoke(spider, method, flag, id, vipFlags))
                .orElse(null);
    }

    public boolean manualVideoCheck(Object spider) {
        return (boolean) getMethod(spider, "manualVideoCheck")
                .map(method -> invoke(spider, method))
                .orElse(false);
    }

    public boolean isVideoFormat(Object spider, String url) {
        return (boolean) getMethod(spider, "isVideoFormat", String.class)
                .map(method -> invoke(spider, method, url))
                .orElse(false);
    }

    @Nullable
    public Object[] proxyLocal(Object spider, Map<String, String> params) {
        return getMethod(spider, "proxyLocal", Map.class)
                .map(method -> SpiderInvokeUtil.<Object[]>invoke(spider, method, params))
                .orElse(null);
    }

    public void destroy(Object spider) {
        getMethod(spider, "destroy")
                .ifPresent(method -> invoke(spider, method));
    }

    @Nullable
    public OkHttpClient client(Object spider) {
        return getMethod(spider, "client")
                .map(method -> SpiderInvokeUtil.<OkHttpClient>invoke(spider, method))
                .orElse(null);
    }

    private Optional<Method> getMethod(Object spider, String methodName, Class<?> ... parameterTypes) {
        Class<?> clazz = spider.getClass();

        try {
            return Optional.of(clazz.getMethod(methodName, parameterTypes));
        } catch (Throwable e) {
            log.error("FreeBox spider exception", e);
            Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.ERROR_SPIDER_INVOKE_FAILED));

            return Optional.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T invoke(Object spider, Method method, Object ... args) {
        Object result;

        try {
            result = method.invoke(spider, args);

            return result == null ? null : (T) result;
        } catch (Throwable e) {
            log.error("FreeBox spider exception", e);
            Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.ERROR_SPIDER_INVOKE_FAILED));
        }

        return null;
    }
}
