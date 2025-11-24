package io.knifer.freebox.model.bo;

import io.knifer.freebox.constant.Architecture;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.Platform;
import io.knifer.freebox.helper.SystemHelper;
import io.knifer.freebox.model.domain.UpgradeConfig;
import io.knifer.freebox.util.CollectionUtil;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;

/**
 * 更新检查结果
 *
 * @author Knifer
 */
@Data
public class UpgradeCheckResultBO {

    private boolean hasNewVersion;

    private UpgradeConfig upgradeConfig;

    private UpgradeConfig.ReleaseFileInfo availableReleaseFileInfo;

    public static UpgradeCheckResultBO from(UpgradeConfig upgradeConfig) {
        UpgradeCheckResultBO result = new UpgradeCheckResultBO();
        String appVersionCode = BaseResources.X_PROPERTIES.getProperty(BaseValues.X_APP_VERSION_CODE);
        List<UpgradeConfig.ReleaseFileInfo> releaseFiles = upgradeConfig.getReleaseFiles();
        Platform platform;
        Architecture architecture;

        result.setUpgradeConfig(upgradeConfig);
        if (CollectionUtil.isEmpty(releaseFiles)) {
            result.setHasNewVersion(false);
        } else {
            platform = SystemHelper.getPlatform();
            architecture = SystemHelper.getArchitecture();
            releaseFiles.stream()
                    .filter(
                            releaseFileInfo -> releaseFileInfo.getPlatform() == platform &&
                                    releaseFileInfo.getArchitecture() == architecture
                    )
                    .findFirst()
                    .ifPresentOrElse(
                            fi -> {
                                result.setHasNewVersion(
                                        ObjectUtils.defaultIfNull(
                                                upgradeConfig.getVersionCode(), 0
                                        ) > Integer.parseInt(appVersionCode)
                                );
                                result.setAvailableReleaseFileInfo(fi);
                            },
                            () -> result.setHasNewVersion(false)
                    );
        }

        return result;
    }
}
