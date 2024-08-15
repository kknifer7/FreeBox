package io.knifer.freebox.service;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.domain.Config;
import io.knifer.freebox.util.GsonUtil;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 保存配置服务
 *
 * @author Knifer
 */
@AllArgsConstructor
public class SaveConfigService extends Service<Void> {

    private static final Path CONFIG_PATH = ConfigHelper.getConfigPath();

    private Config config;

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                saveConfigOnDisk();
                Platform.runLater(() -> ToastHelper.showSuccess(I18nKeys.SETTINGS_SAVED));

                return null;
            }
        };
    }

    private void saveConfigOnDisk() {
        try {
            Files.writeString(CONFIG_PATH, GsonUtil.toJson(config));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
