package io.knifer.freebox.spider;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.github.catvod.crawler.spider.Spider;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.StorageHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.domain.FreeBoxApiConfig;
import io.knifer.freebox.spider.js.JSSpider;
import io.knifer.freebox.util.CastUtil;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.catvod.SpiderInvokeUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import javafx.application.Platform;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 爬虫Jar Loader
 * 参考：<a href="https://github.com/Greatwallcorner/TV-Multiplatform/blob/main/composeApp/src/commonMain/kotlin/com/corner/catvodcore/loader/JarLoader.kt#L114">TV-Multiplatform</a>
 * @author Knifer
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@Singleton
public class SpiderJarLoader {

    private final ConcurrentHashMap<String, URLClassLoader> loaders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Method> methods = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> spiders = new ConcurrentHashMap<>();
    @Setter
    private String recent = null;
    @Setter
    private FreeBoxApiConfig apiConfig = null;

    // 使用Provider注入，避免循环依赖
    private final Provider<Context> contextProvider;

    private final static String SPIDER_PACKAGE_NAME = "com.github.catvod.spider";
    private final static String SPIDER_PROXY_CLASS_NAME = SPIDER_PACKAGE_NAME + ".Proxy";
    private final static String SPIDER_INIT_CLASS_NAME = SPIDER_PACKAGE_NAME + ".Init";
    private final static Path SPIDER_CACHE_PATH = StorageHelper.getSpiderCachePath();

    static {
        if (!Files.exists(SPIDER_CACHE_PATH)) {
            try {
                Files.createDirectories(SPIDER_CACHE_PATH);
            } catch (IOException e) {
                ToastHelper.showException(e);
            }
        }
    }

    @Nullable
    public Object getSpider() {
        return spiders.get(recent);
    }

    public Object getSpider(String key, String api, String ext, String jar) {
        try {
            boolean jsFlag = api.endsWith(".js");
            String jaKey = DigestUtil.md5Hex(jsFlag ? api : jar);
            String spKey = jaKey + key;
            Object spider;

            if (spiders.containsKey(spKey)) {
                return spiders.get(spKey);
            }
            recent = jaKey;
            if (jsFlag) {
                spider = spiders.get(spKey);
                if (spider == null) {
                    if (!loadJar(spKey, api, true)) {
                        return Spider.getEmpty();
                    }
                    spider = spiders.get(spKey);
                }
                if (ext == null) {
                    ext = StringUtils.EMPTY;
                }
            } else {
                if (loaders.get(jaKey) == null) {
                    if (!loadJar(jaKey, jar, false)) {
                        return Spider.getEmpty();
                    }
                }
                URLClassLoader loader = loaders.get(jaKey);
                if (loader == null) {
                    return Spider.getEmpty();
                }
                String classPath = SPIDER_PACKAGE_NAME + api.replace("csp_", ".");
                spider = loader.loadClass(classPath).getDeclaredConstructor().newInstance();
            }
            if (spider == null || !SpiderInvokeUtil.init(spider, ext)) {
                spider = Spider.getEmpty();
            }
            spiders.put(spKey, spider);

            return spider;
        } catch (Exception e){
            Platform.runLater(() -> ToastHelper.showException(e));

            return Spider.getEmpty();
        }
    }

    public boolean loadJar(String key, String spider, boolean jsFlag) {
        String[] texts;
        String md5;
        String jar;
        Path jarPath;

        if (StringUtils.isBlank(spider)) {

            return false;
        }
        texts = spider.split(";md5;");
        md5 = texts.length > 1 ? texts[1].trim() : StringUtils.EMPTY;
        jar = texts[0];

        // 可以避免重复下载
        if (!md5.isEmpty() && Objects.equals(parseJarUrl(jar), md5)) {

            return load(key, Paths.get(parseJarUrl(jar)), jsFlag);
        } else if (jar.startsWith("file")) {

            return load(key, Paths.get(jar.replace("file:///", StringUtils.EMPTY)), jsFlag);
        } else if (jar.startsWith("http")) {
            jarPath = download(jar);
            if (jarPath == null) {

                return false;
            }

            return load(key, jarPath, jsFlag);
        } else if (jar.startsWith("assets")) {

            return load(key, Paths.get(jar.replace("assets://", StringUtils.EMPTY)), jsFlag);
        } else {

            return loadJar(key, convertUrl(apiConfig.getUrl(), jar), jsFlag);
        }
    }

    /**
     * 解析jar路径为绝对路径
     */
    private String parseJarUrl(String jar) {
        if (jar.startsWith("file") || jar.startsWith("http")) {
            return jar;
        }

        return convertUrl(apiConfig.getUrl(), jar);
    }

    private String convertUrl(String baseUrl, String refUrl) {
        try {
            return new URI(baseUrl.replace("file://", "file:/")
                    .replace("\\", "/"))
                    .resolve(refUrl)
                    .toString();
        } catch (URISyntaxException e) {
            Platform.runLater(() -> ToastHelper.showException(e));

            return StringUtils.EMPTY;
        }
    }

    private boolean load(String key, Path jar, boolean jsFlag) {
        log.info("load jar {}", jar);
        if (jsFlag) {
            spiders.put(key, new JSSpider(key, jar));

            return true;
        }
        if (!isJarAvailable(jar)) {
            log.info("invalid jar: {}", jar);
            Platform.runLater(() -> ToastHelper.showErrorAlert(
                    I18nKeys.ERROR,
                    I18nKeys.TV_ERROR_INVALID_SPIDER_JAR,
                    null
            ));

            return false;
        }
        try {
            loaders.put(key, new URLClassLoader(new URL[]{jar.toUri().toURL()}, this.getClass().getClassLoader()));
        } catch (MalformedURLException e) {
            Platform.runLater(() -> ToastHelper.showException(e));

            return false;
        }
        putProxy(key);
        invokeInit(key);

        return true;
    }

    private boolean isJarAvailable(Path jarPath) {
        Enumeration<JarEntry> entries;
        JarEntry entry;
        String name;

        try (JarFile jarFile = new JarFile(jarPath.toFile())) {
            entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                entry = entries.nextElement();
                name = entry.getName();
                if (name.endsWith(".dex") || name.contains("classes.dex")) {
                    log.warn("{} is not available", name);

                    return false;
                }
            }
        } catch (IOException e) {
            log.error("check jar file error", e);

            return false;
        }

        return true;
    }

    private void putProxy(String key) {
        URLClassLoader classLoader = loaders.get(key);
        Class<?> clazz;
        Method method;

        if (classLoader == null) {
            throw new AssertionError();
        }
        try {
            clazz = classLoader.loadClass(SPIDER_PROXY_CLASS_NAME);
            method = clazz.getMethod("proxy", Map.class);
            methods.put(key, method);
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            Platform.runLater(() -> ToastHelper.showException(e));
        }
    }

    /**
     * 执行Java爬虫的初始化Init类
     * @param key 爬虫key（jaKey）
     * @return 是否初始化成功
     */
    private boolean invokeInit(String key) {
        URLClassLoader classLoader = loaders.get(key);
        Class<?> clazz;
        Method method;

        if (classLoader == null) {
            throw new AssertionError();
        }
        try {
            clazz = classLoader.loadClass(SPIDER_INIT_CLASS_NAME);
            method = clazz.getMethod("init");
            method.invoke(clazz);

            return true;
        } catch (
                ClassNotFoundException |
                NoSuchMethodException |
                IllegalAccessException |
                InvocationTargetException e
        ) {
            Platform.runLater(() -> ToastHelper.showException(e));

            return false;
        }
    }

    @Nullable
    private Path download(String jar) {
        log.info("download jar: {}", jar);
        try {
            return Files.write(SPIDER_CACHE_PATH.resolve(DigestUtil.md5Hex(jar)), HttpUtil.getFile(jar));
        } catch (IOException e) {
            log.error("download jar error", e);
            Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.TV_ERROR_LOAD_SPIDER_JAR_FAILED));

            return null;
        }
    }

    public Object[] proxyInvoke(Map<String, String> params) {
        String doVal = params.get("do");
        Object recentSpider;
        Method proxyMethod;
        CompletableFuture<Object[]> resultFuture;
        Object[] result;

        if ("proxy".equalsIgnoreCase(doVal)) {
            // java 爬虫代理
            proxyMethod = methods.get(recent);
            try {
                result = CastUtil.cast(proxyMethod.invoke(null, params));
            } catch (IllegalAccessException | InvocationTargetException e) {
                result = ArrayUtils.EMPTY_OBJECT_ARRAY;
                log.warn("proxyInvoke error, proxyMethod={}", proxyMethod, e);
            }
        } else {
            // 其他语言爬虫代理
            recentSpider = spiders.get(recent);
            if (recentSpider == null) {
                result = ArrayUtils.EMPTY_OBJECT_ARRAY;
                log.warn("proxyInvoke error, can't find recent spider. recent={}", recent);
            } else {
                resultFuture = new CompletableFuture<>();
                Platform.runLater(
                        () -> contextProvider.get().getSpiderTemplate().proxy(resultFuture::complete, params)
                );
                try {
                    result = resultFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    result = ArrayUtils.EMPTY_OBJECT_ARRAY;
                    log.warn("proxyInvoke error, recentSpider={}", recentSpider, e);
                }
            }
        }

        return result;
    }

    public void destroy() {
        log.info("destroy SpiderJarLoader......");
        try {
            spiders.values().forEach(SpiderInvokeUtil::destroy);
        } catch (Exception e) {
            log.warn("destroy spider error", e);
        }
        for (URLClassLoader classLoader : loaders.values()) {
            try {
                classLoader.close();
            } catch (IOException ignored) {}
        }
        FileUtil.clean(SPIDER_CACHE_PATH.toString());
    }
}
