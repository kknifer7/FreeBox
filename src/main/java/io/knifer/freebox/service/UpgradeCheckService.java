package io.knifer.freebox.service;

import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.bo.UpgradeCheckResultBO;
import io.knifer.freebox.model.domain.UpgradeConfig;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.json.GsonUtil;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
                    jsonContent = HttpUtil.getAsync(PROPS_URL).get(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Platform.runLater(() -> ToastHelper.showException(e));

                    return null;
                } catch (TimeoutException | ExecutionException e) {
                    Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.MESSAGE_AUTO_CHECK_UPGRADE_FAILED));

                    return null;
                }
                upgradeConfig = GsonUtil.fromJson(jsonContent, UpgradeConfig.class);
                result = UpgradeCheckResultBO.from(upgradeConfig);

                log.info("upgrade check result: {}", result);

                return result;
            }
        };
    }
}
