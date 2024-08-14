package io.knifer.freebox.helper;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.model.domain.Config;
import io.knifer.freebox.util.GsonUtil;
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

    private static final Path CONFIG_PATH = Path.of("config", "config.json");

    private static Config config = loadConfigFromLocalPath();

    private static final AtomicBoolean updateFlag = new AtomicBoolean(false);

    private static final Runnable SAVE_CONFIG_RUNNABLE = () -> {
        if (updateFlag.getAndSet(false)) {
            saveConfigOnDisk();
        }
        ToastHelper.showSuccess(I18nKeys.SETTINGS_SAVED);
    };

    public void loadConfig() {
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
                // TODO 新建Config
                Files.createDirectories(CONFIG_PATH.getParent());
                Files.writeString(CONFIG_PATH, GsonUtil.toJson(config));

                return config;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void markUpdating() {
        updateFlag.set(true);
    }

    public void saveConfig() {
        Platform.runLater(SAVE_CONFIG_RUNNABLE);
    }

    private void saveConfigOnDisk() {
        try {
            Files.writeString(CONFIG_PATH, GsonUtil.toJson(config));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
