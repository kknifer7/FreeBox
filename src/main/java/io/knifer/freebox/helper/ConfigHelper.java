package io.knifer.freebox.helper;

import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.model.domain.Config;
import io.knifer.freebox.service.LoadConfigService;
import io.knifer.freebox.service.SaveConfigService;
import io.knifer.freebox.util.GsonUtil;
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

    private static final Path CONFIG_PATH = Path.of("config", "config.json");

    private static Config config;

    private static final AtomicBoolean updateFlag = new AtomicBoolean(false);

    static {
        new LoadConfigService().start();
    }

    public void setServiceIPv4(String serviceIPv4) {
        assertIfConfigLoaded();
        config.setServiceIPv4(serviceIPv4);
    }

    public String getServiceIPv4() {
        assertIfConfigLoaded();

        return config.getServiceIPv4();
    }

    public void setHttpPort(Integer httpPort) {
        assertIfConfigLoaded();
        config.setHttpPort(httpPort);
    }

    public Integer getHttpPort() {
        assertIfConfigLoaded();

        return config.getHttpPort();
    }

    public Boolean getAutoStartHttp() {
        assertIfConfigLoaded();

        return config.getAutoStartHttp();
    }

    public void setAutoStartHttp(Boolean autoStartHttp) {
        assertIfConfigLoaded();
        config.setAutoStartHttp(autoStartHttp);
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
        Config config;
        String configJson;

        if (Files.exists(CONFIG_PATH)) {
            try {
                configJson = Files.readString(CONFIG_PATH);

                return GsonUtil.fromJson(configJson, Config.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                config = new Config();
                config.setUuid(UUID.randomUUID().toString());
                config.setHttpPort(BaseValues.DEFAULT_HTTP_PORT);
                config.setWsPort(BaseValues.DEFAULT_WS_PORT);
                config.setAutoStartHttp(false);
                config.setAutoStartWs(false);
                Files.createDirectories(CONFIG_PATH.getParent());
                Files.writeString(CONFIG_PATH, GsonUtil.toJson(config));

                return config;
            } catch (IOException e) {
                throw new RuntimeException(e);
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
    public void checkAndSave() {
        if (updateFlag.getAndSet(false)) {
            saveAnyWay();
        }
    }

    /**
     * 立即保存配置
     */
    public void saveAnyWay() {
        updateFlag.set(false);
        new SaveConfigService(config).start();
    }

    public Path getConfigPath() {
        return CONFIG_PATH;
    }
}
