package io.knifer.freebox.service;

import cn.hutool.core.io.IoUtil;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.bo.UpgradeCheckResultBO;
import io.knifer.freebox.model.domain.UpgradeConfig;
import io.knifer.freebox.util.json.GsonUtil;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URL;

/**
 * 更新检查服务
 *
 * @author Knifer
 */
@Slf4j
public class UpgradeCheckService extends Service<UpgradeCheckResultBO> {

    private final static String PROPS_URL = BaseResources.X_PROPERTIES.getProperty(BaseValues.X_UPGRADE_CONFIG_URL);

    @Override
    protected Task<UpgradeCheckResultBO> createTask() {
        return new Task<>() {
            @Override
            protected UpgradeCheckResultBO call() {
                String jsonContent;
                UpgradeConfig upgradeConfig;
                UpgradeCheckResultBO result;

                try {
                    jsonContent = fetchUpgradeConfig();
                    upgradeConfig = GsonUtil.fromJson(jsonContent, UpgradeConfig.class);
                } catch (Exception e) {
                    Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.MESSAGE_AUTO_CHECK_UPGRADE_FAILED));

                    return null;
                }
                result = UpgradeCheckResultBO.from(upgradeConfig);

                log.info("upgrade check result: {}", result);

                return result;
            }
        };
    }

    private String fetchUpgradeConfig() {
        try {
            // 以系统原生方式请求，用框架的话会触发SourceForge的反爬
            return IoUtil.readUtf8(new URL(PROPS_URL).openStream());
        } catch (IOException ignored) {}

        return StringUtils.EMPTY;
    }
}
