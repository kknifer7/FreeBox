package io.knifer.freebox.service;

import io.knifer.freebox.helper.ConfigHelper;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

/**
 * 读取配置服务
 *
 * @author Knifer
 */
@Slf4j
public class LoadConfigService extends Service<Void> {
    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                ConfigHelper.loadConfig();

                return null;
            }
        };
    }

    @Override
    protected void succeeded() {
        log.info("LoadConfigService succeeded");
    }
}
