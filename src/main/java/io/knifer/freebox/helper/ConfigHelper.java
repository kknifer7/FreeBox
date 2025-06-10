package io.knifer.freebox.helper;

import io.knifer.freebox.constant.AppVersions;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.model.domain.Config;
import io.knifer.freebox.service.SaveConfigService;
import io.knifer.freebox.util.json.GsonUtil;
import javafx.application.Platform;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 设置
 *
 * @author Knifer
 */
@UtilityClass
public class ConfigHelper {
    private static final Path CONFIG_PATH = StorageHelper.getLocalStoragePath()
            .resolve(Path.of("config", "config.json"));

    private volatile static Config config;

    private static final AtomicBoolean updateFlag = new AtomicBoolean(false);

    public synchronized void setServiceIPv4(String serviceIPv4) {
        assertIfConfigLoaded();
        config.setServiceIPv4(serviceIPv4);
    }

    public String getServiceIPv4() {
        assertIfConfigLoaded();

        return config.getServiceIPv4();
    }

    public synchronized void setHttpPort(Integer httpPort) {
        assertIfConfigLoaded();
        config.setHttpPort(httpPort);
    }

    public Integer getHttpPort() {
        assertIfConfigLoaded();

        return config.getHttpPort();
    }

    public synchronized void setAutoStartHttp(Boolean autoStartHttp) {
        assertIfConfigLoaded();
        config.setAutoStartHttp(autoStartHttp);
    }

    public Boolean getAutoStartHttp() {
        assertIfConfigLoaded();

        return config.getAutoStartHttp();
    }

    public synchronized void setAutoStartWs(Boolean autoStartWs) {
        assertIfConfigLoaded();
        config.setAutoStartWs(autoStartWs);
    }

    public Boolean getAutoStartWs() {
        assertIfConfigLoaded();

        return config.getAutoStartWs();
    }

    public synchronized void setWsPort(Integer wsPort) {
        assertIfConfigLoaded();
        config.setWsPort(wsPort);
    }

    public Integer getWsPort() {
        assertIfConfigLoaded();

        return config.getWsPort();
    }

    public String getAppVersion() {
        assertIfConfigLoaded();

        return config.getAppVersion();
    }

    public void setAppVersion(String appVersion) {
        assertIfConfigLoaded();
        config.setAppVersion(appVersion);
    }

    private void assertIfConfigLoaded() {
        if (config == null) {
            throw new IllegalStateException("config is not loaded");
        }
    }

    public synchronized void loadConfig() {
        config = loadConfigFromLocalPath();
    }

    private Config loadConfigFromLocalPath() {
        Config configLoaded;
        String configJson;

        if (Files.exists(CONFIG_PATH)) {
            try {
                configJson = Files.readString(CONFIG_PATH);
                configLoaded = GsonUtil.fromJson(configJson, Config.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                configLoaded = new Config();
                configLoaded.setUuid(UUID.randomUUID().toString());
                configLoaded.setAppVersion(BaseResources.X_PROPERTIES.getProperty(
                        BaseValues.X_APP_VERSION, AppVersions.ONE_ZERO_ZERO
                ));
                configLoaded.setServiceIPv4(BaseValues.ANY_LOCAL_IP);
                configLoaded.setHttpPort(BaseValues.DEFAULT_HTTP_PORT);
                configLoaded.setWsPort(BaseValues.DEFAULT_WS_PORT);
                configLoaded.setAutoStartHttp(true);
                configLoaded.setAutoStartWs(true);
                Files.createDirectories(CONFIG_PATH.getParent());
                Files.writeString(CONFIG_PATH, GsonUtil.toJson(configLoaded));
            } catch (IOException e) {
                Platform.runLater(() -> ToastHelper.showException(e));

                return null;
            }
        }

        return configLoaded;
    }

    public void markToUpdate() {
        setUpdateFlag(true);
    }

    public void unmarkToUpdate() {
        setUpdateFlag(false);
    }

    private void setUpdateFlag(boolean updateFlag) {
        ConfigHelper.updateFlag.set(updateFlag);
    }

    /**
     * 检测并保存配置
     */
    public void checkAndSave() {
        if (updateFlag.getAndSet(false)) {
            saveAnyWay();
        }
    }

    /**
     * 立即保存配置
     */
    public synchronized void saveAnyWay() {
        updateFlag.set(false);
        new SaveConfigService(config).start();
    }

    /**
     * 立即保存配置
     */
    public synchronized void saveAnyWay(Runnable callback) {
        SaveConfigService service;

        updateFlag.set(false);
        service = new SaveConfigService(config);
        service.setOnSucceeded(evt -> callback.run());
        service.start();
    }

    public Path getConfigPath() {
        return CONFIG_PATH;
    }
}
