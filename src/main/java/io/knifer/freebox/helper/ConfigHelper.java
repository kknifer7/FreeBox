package io.knifer.freebox.helper;

import cn.hutool.core.collection.CollUtil;
import io.knifer.freebox.constant.*;
import io.knifer.freebox.model.domain.Config;
import io.knifer.freebox.service.SaveConfigService;
import io.knifer.freebox.util.json.GsonUtil;
import javafx.application.Platform;
import javafx.scene.text.Font;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.math.NumberUtils;

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

    public synchronized void setAppVersion(String appVersion) {
        assertIfConfigLoaded();
        config.setAppVersion(appVersion);
    }

    public Integer getAppVersionCode() {
        assertIfConfigLoaded();

        return config.getAppVersionCode();
    }

    public synchronized void setAppVersionCode(Integer appVersionCode) {
        assertIfConfigLoaded();
        config.setAppVersionCode(appVersionCode);
    }

    public Boolean getAutoCheckUpgrade() {
        assertIfConfigLoaded();

        return config.getAutoCheckUpgrade();
    }

    public synchronized void setAutoCheckUpgrade(Boolean autoCheckUpgrade) {
        assertIfConfigLoaded();
        config.setAutoCheckUpgrade(autoCheckUpgrade);
    }

    public Boolean getShowLicense() {
        assertIfConfigLoaded();

        return config.getShowLicense();
    }

    public synchronized void setShowLicense(Boolean showLicense) {
        assertIfConfigLoaded();
        config.setShowLicense(showLicense);
    }

    public String getUsageFontFamily() {
        assertIfConfigLoaded();

        return config.getUsageFontFamily();
    }

    public synchronized void setUsageFontFamily(String usageFontFamily) {
        assertIfConfigLoaded();
        config.setUsageFontFamily(usageFontFamily);
    }

    public Boolean getAdFilter() {
        assertIfConfigLoaded();

        return config.getAdFilter();
    }

    public synchronized void setAdFilter(Boolean adFilter) {
        assertIfConfigLoaded();
        config.setAdFilter(adFilter);
    }

    public Double getAdFilterDynamicThresholdFactor() {
        assertIfConfigLoaded();

        return config.getAdFilterDynamicThresholdFactor();
    }

    public synchronized void setAdFilterDynamicThresholdFactor(Double adFilterDynamicThresholdFactor) {
        assertIfConfigLoaded();
        config.setAdFilterDynamicThresholdFactor(adFilterDynamicThresholdFactor);
    }

    public PlayerType getPlayerType() {
        assertIfConfigLoaded();

        return config.getPlayerType();
    }

    public synchronized void setPlayerType(PlayerType playerType) {
        assertIfConfigLoaded();
        config.setPlayerType(playerType);
    }

    public String getMpvPath() {
        assertIfConfigLoaded();

        return config.getMpvPath();
    }

    public synchronized void setMpvPath(String mpvPath) {
        assertIfConfigLoaded();
        config.setMpvPath(mpvPath);
    }

    public VideoPlaybackTrigger getVideoPlaybackTrigger() {
        assertIfConfigLoaded();

        return config.getVideoPlaybackTrigger();
    }

    public synchronized void setVideoPlaybackTrigger(VideoPlaybackTrigger videoPlaybackTrigger) {
        assertIfConfigLoaded();
        config.setVideoPlaybackTrigger(videoPlaybackTrigger);
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
        String versionCodeStr;

        try {
            if (Files.exists(CONFIG_PATH)) {
                configJson = Files.readString(CONFIG_PATH);
                configLoaded = GsonUtil.fromJson(configJson, Config.class);
                fixConfigIfNeeded(configLoaded);
            } else {
                configLoaded = new Config();
                configLoaded.setUuid(UUID.randomUUID().toString());
                configLoaded.setAppVersion(BaseResources.X_PROPERTIES.getProperty(
                        BaseValues.X_APP_VERSION, AppVersions.ONE_ZERO_ZERO
                ));
                versionCodeStr = BaseResources.X_PROPERTIES.getProperty(BaseValues.X_APP_VERSION_CODE);
                configLoaded.setAppVersionCode(
                        NumberUtils.isCreatable(versionCodeStr) ? Integer.parseInt(versionCodeStr) : 0
                );
                configLoaded.setShowLicense(true);
                configLoaded.setAutoCheckUpgrade(true);
                configLoaded.setServiceIPv4(BaseValues.ANY_LOCAL_IP);
                configLoaded.setHttpPort(BaseValues.DEFAULT_HTTP_PORT);
                configLoaded.setWsPort(BaseValues.DEFAULT_WS_PORT);
                configLoaded.setAutoStartHttp(true);
                configLoaded.setAutoStartWs(true);
                configLoaded.setUsageFontFamily(Font.getDefault().getFamily());
                configLoaded.setAdFilter(true);
                configLoaded.setAdFilterDynamicThresholdFactor(-1D);
                configLoaded.setPlayerType(PlayerType.VLC);
                configLoaded.setVideoPlaybackTrigger(VideoPlaybackTrigger.SINGLE_CLICK);
                Files.createDirectories(CONFIG_PATH.getParent());
                Files.writeString(CONFIG_PATH, GsonUtil.toJson(configLoaded));
            }
        } catch (IOException e) {
            Platform.runLater(() -> ToastHelper.showException(e));

            return null;
        }

        return configLoaded;
    }

    private void fixConfigIfNeeded(Config config) {
        String usageFontFamily = config.getUsageFontFamily();
        Boolean adFilter = config.getAdFilter();
        Double adFilterDynamicThresholdFactor = config.getAdFilterDynamicThresholdFactor();
        boolean needSave = false;

        if (usageFontFamily == null || !CollUtil.contains(Font.getFamilies(), usageFontFamily)) {
            config.setUsageFontFamily(Font.getDefault().getFamily());
            needSave = true;
        }
        if (adFilter == null) {
            config.setAdFilter(true);
            needSave = true;
        }
        if (adFilterDynamicThresholdFactor == null) {
            config.setAdFilterDynamicThresholdFactor(-1D);
            needSave = true;
        }
        if (config.getPlayerType() == null) {
            config.setPlayerType(PlayerType.VLC);
            needSave = true;
        }
        if (config.getVideoPlaybackTrigger() == null) {
            config.setVideoPlaybackTrigger(VideoPlaybackTrigger.SINGLE_CLICK);
            needSave = true;
        }
        if (needSave) {
            try {
                Files.writeString(CONFIG_PATH, GsonUtil.toJson(config));
            } catch (IOException e) {
                Platform.runLater(() -> ToastHelper.showException(e));
            }
        }
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
    public boolean checkAndSave() {
        boolean saved;

        saved = updateFlag.getAndSet(false);
        if (saved) {
            saveAnyWay();
        }

        return saved;
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
