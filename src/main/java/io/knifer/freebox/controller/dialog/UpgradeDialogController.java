package io.knifer.freebox.controller.dialog;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.controller.BaseController;
import io.knifer.freebox.helper.HostServiceHelper;
import io.knifer.freebox.helper.LoadingHelper;
import io.knifer.freebox.helper.StorageHelper;
import io.knifer.freebox.helper.WindowHelper;
import io.knifer.freebox.model.bo.UpgradeCheckResultBO;
import io.knifer.freebox.model.domain.UpgradeConfig;
import io.knifer.freebox.service.DownloadService;
import io.knifer.freebox.util.FormattingUtil;
import io.knifer.freebox.util.ValidationUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * 更新对话框
 *
 * @author Knifer
 */
@Slf4j
public class UpgradeDialogController extends BaseController {

    @FXML
    private VBox root;
    @FXML
    private Label versionLabel;
    @FXML
    private Label sizeLabel;
    @FXML
    private TextFlow changelogTextFlow;
    @FXML
    private Label progressLabel;
    @FXML
    private ProgressBar downloadProgressBar;
    @FXML
    private Button upgradeButton;

    private final BooleanProperty upgradingProperty = new SimpleBooleanProperty(false);
    private File storagePath;
    private DownloadService downloadService;

    @FXML
    private void initialize() {
        Platform.runLater(() -> {
            UpgradeCheckResultBO upgradeCheckResult = getData();
            UpgradeConfig upgradeConfig = upgradeCheckResult.getUpgradeConfig();
            UpgradeConfig.ReleaseFileInfo releaseFileInfo = upgradeCheckResult.getAvailableReleaseFileInfo();

            storagePath = StorageHelper.getTempStoragePath()
                    .resolve(releaseFileInfo.getFileName())
                    .toFile();
            versionLabel.setText(upgradeConfig.getVersion());
            sizeLabel.setText(FormattingUtil.sizeFormat(releaseFileInfo.getFileSize()));
            changelogTextFlow.getChildren().add(new Text(upgradeConfig.getChangelog()));

            upgradeButton.disableProperty().bind(upgradingProperty);
        });
    }

    @FXML
    private void onCancelButtonAction() {
        cancelUpgrade();
        WindowHelper.close(root);
    }

    private void cancelUpgrade() {
        if (upgradingProperty.get()) {
            downloadService.cancel();
            FileUtil.del(storagePath);
        }
    }

    @FXML
    private void onManualUpgradeButtonAction() {
        UpgradeConfig upgradeConfig = getUpgradeConfig();
        String releaseUrl = upgradeConfig.getReleaseUrl();

        if (ValidationUtil.isURL(releaseUrl)) {
            HostServiceHelper.showDocument(releaseUrl);
        }
        if (!upgradingProperty.get()) {
            WindowHelper.close(root);
        }
    }

    private UpgradeConfig getUpgradeConfig() {
        UpgradeCheckResultBO upgradeCheckResult = getData();

        return upgradeCheckResult.getUpgradeConfig();
    }

    @FXML
    private void onUpgradeButtonAction() {
        upgradingProperty.set(true);
        downloadService = new DownloadService(
                getReleaseFileInfo().getDownloadLink(),
                storagePath,
                () -> log.info("upgrade download start"),
                totalAndProgressSize -> {
                    Long total = totalAndProgressSize.getKey();
                    Long progressSize = totalAndProgressSize.getValue();

                    Platform.runLater(() -> {
                        progressLabel.setText(String.format(
                                "%s / %s",
                                FormattingUtil.sizeFormat(progressSize),
                                FormattingUtil.sizeFormat(total)
                        ));
                        downloadProgressBar.setProgress((double) progressSize / total);
                    });
                },
                () -> {
                    log.info("upgrade download complete, installing......");
                    RuntimeUtil.exec("msiexec", "/i", "\"" + storagePath.getAbsolutePath() + "\"");
                    Platform.runLater(() -> {
                        upgradingProperty.set(false);
                        LoadingHelper.showLoading(
                                WindowHelper.getStage(root),
                                I18nKeys.MESSAGE_UPGRADE_QUIT_LOADING
                        );
                        Context.INSTANCE.destroy();
                    });
                }
        );
        downloadService.start();
    }

    private UpgradeConfig.ReleaseFileInfo getReleaseFileInfo() {
        UpgradeCheckResultBO upgradeCheckResult = getData();

        return upgradeCheckResult.getAvailableReleaseFileInfo();
    }
}
