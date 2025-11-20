package io.knifer.freebox.spider;

import cn.hutool.core.io.FileUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.github.catvod.spider.Spider;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.StorageHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.domain.FreeBoxApiConfig;
import io.knifer.freebox.util.CastUtil;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.catvod.SpiderInvokeUtil;
import javafx.application.Platform;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 爬虫Jar Loader
 * 参考：<a href="https://github.com/Greatwallcorner/TV-Multiplatform/blob/main/composeApp/src/commonMain/kotlin/com/corner/catvodcore/loader/JarLoader.kt#L114">TV-Multiplatform</a>
 * @author Knifer
 */
@Slf4j
public class SpiderJarLoader {

    private final ConcurrentHashMap<String, URLClassLoader> loaders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Method> methods = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object> spiders = new ConcurrentHashMap<>();
    private String recent = null;
    @Setter
    private FreeBoxApiConfig apiConfig = null;

    private final static String SPIDER_PACKAGE_NAME = "com.github.catvod.spider";
    private final static String SPIDER_PROXY_CLASS_NAME = SPIDER_PACKAGE_NAME + ".Proxy";
    private final static String SPIDER_INIT_CLASS_NAME = SPIDER_PACKAGE_NAME + ".Init";
    private final static Path SPIDER_CACHE_PATH = StorageHelper.getSpiderCachePath();
    private final static SpiderJarLoader INSTANCE = new SpiderJarLoader();

    static {
        if (!Files.exists(SPIDER_CACHE_PATH)) {
            try {
                Files.createDirectories(SPIDER_CACHE_PATH);
            } catch (IOException e) {
                ToastHelper.showException(e);
            }
        }
    }

    public static SpiderJarLoader getInstance() {
        return INSTANCE;
    }

    public Object getSpider(String key, String api, String ext, String jar) {
        try {
            String jaKey = DigestUtil.md5Hex(jar);
            String spKey = jaKey + key;

            if (spiders.containsKey(spKey)) {
                return spiders.get(spKey);
            }
            if (loaders.get(jaKey) == null) {
                if (!loadJar(jaKey, jar)) {
                    return new Spider();
                }
            }
            recent = jaKey;
            URLClassLoader loader = loaders.get(jaKey);
            if (loader == null) {
                return new Spider();
            }
            String classPath = SPIDER_PACKAGE_NAME + api.replace("csp_", ".");
            Object spider = loader.loadClass(classPath).getDeclaredConstructor().newInstance();
            SpiderInvokeUtil.init(spider, ext);
            spiders.put(spKey, spider);

            return spider;
        } catch (Exception e){
            Platform.runLater(() -> ToastHelper.showException(e));

            return new Spider();
        }
    }

    public boolean loadJar(String key, String spider) {
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
        if(!md5.isEmpty() && Objects.equals(parseJarUrl(jar), md5)){

            return load(key, Paths.get(parseJarUrl(jar)));
        }else if (jar.startsWith("file")) {

            return load(key, Paths.get(jar.replace("file:///", StringUtils.EMPTY)));
        } else if (jar.startsWith("http")) {
            jarPath = download(jar);
            if (jarPath == null) {

                return false;
            }

            return load(key, jarPath);
        } else {

            return loadJar(key, convertUrl(apiConfig.getUrl(), jar));
        }
    }

    /**
     * 如果在配置文件种使用的相对路径，下载的时候使用的全路径 如果的判断md5是否一致的时候使用相对路径 就会造成重复下载
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

    private boolean load(String key, Path jar) {
        log.info("load jar {}", jar);
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

    private void invokeInit(String key) {
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
        } catch (
                ClassNotFoundException |
                NoSuchMethodException |
                IllegalAccessException |
                InvocationTargetException e
        ) {
            Platform.runLater(() -> ToastHelper.showException(e));
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
        Method proxyMethod = methods.get(recent);
        Object[] result;

        try {
            result = CastUtil.cast(proxyMethod.invoke(null, params));
        } catch (IllegalAccessException | InvocationTargetException e) {
            result = ArrayUtils.EMPTY_OBJECT_ARRAY;
            log.warn("proxyInvoke error, proxyMethod={}", proxyMethod, e);
        }

        return result;
    }

    public void destroy() {
        log.info("destroy SpiderJarLoader......");
        for (URLClassLoader classLoader : loaders.values()) {
            try {
                classLoader.close();
            } catch (IOException ignored) {}
        }
        FileUtil.clean(SPIDER_CACHE_PATH.toString());
    }
}
