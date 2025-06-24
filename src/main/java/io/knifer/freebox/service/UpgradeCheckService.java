package io.knifer.freebox.service;

import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.model.bo.UpgradeCheckResultBO;
import io.knifer.freebox.model.domain.UpgradeConfig;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.json.GsonUtil;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.extern.slf4j.Slf4j;

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
                UpgradeConfig upgradeConfig = GsonUtil.fromJson(HttpUtil.get(PROPS_URL), UpgradeConfig.class);

                return UpgradeCheckResultBO.from(upgradeConfig);
            }
        };
    }
}
