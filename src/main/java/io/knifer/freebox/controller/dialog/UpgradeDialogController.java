package io.knifer.freebox.controller.dialog;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RuntimeUtil;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.ButtonTypes;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.controller.BaseController;
import io.knifer.freebox.exception.FBException;
import io.knifer.freebox.helper.*;
import io.knifer.freebox.model.bo.UpgradeCheckResultBO;
import io.knifer.freebox.model.domain.UpgradeConfig;
import io.knifer.freebox.service.DownloadService;
import io.knifer.freebox.util.FormattingUtil;
import io.knifer.freebox.util.ValidationUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
    private Label currentVersionTooLowLabel;
    @FXML
    private ProgressBar downloadProgressBar;
    @FXML
    private Button upgradeButton;

    private final BooleanProperty upgradingProperty = new SimpleBooleanProperty(false);
    private final BooleanProperty currentVersionTooLowProperty = new SimpleBooleanProperty(false);
    private File storagePath;
    private DownloadService downloadService;

    @FXML
    private void initialize() {
        currentVersionTooLowLabel.visibleProperty().bind(currentVersionTooLowProperty);
        currentVersionTooLowLabel.managedProperty().bind(currentVersionTooLowProperty);
        upgradeButton.disableProperty().bind(currentVersionTooLowProperty.or(upgradingProperty));
        Platform.runLater(() -> {
            UpgradeCheckResultBO upgradeCheckResult = getData();
            UpgradeConfig upgradeConfig = upgradeCheckResult.getUpgradeConfig();
            UpgradeConfig.ReleaseFileInfo releaseFileInfo = upgradeCheckResult.getAvailableReleaseFileInfo();
            int currentVersionCode;

            storagePath = StorageHelper.getTempStoragePath()
                    .resolve(releaseFileInfo.getFileName())
                    .toFile();
            versionLabel.setText(upgradeConfig.getVersion());
            sizeLabel.setText(FormattingUtil.sizeFormat(releaseFileInfo.getFileSize()));
            changelogTextFlow.getChildren().add(new Text(upgradeConfig.getChangelog()));

            currentVersionCode = Integer.parseInt(
                    BaseResources.X_PROPERTIES.getProperty(BaseValues.X_APP_VERSION_CODE)
            );
            if (currentVersionCode < upgradeConfig.getMinRequiredVersionCode()) {
                currentVersionTooLowProperty.set(true);
            }
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
                    Platform.runLater(() -> {
                        switch (SystemHelper.getPlatform()) {
                            case WINDOWS -> startInstallOnWindows();
                            case DEB_LINUX, RPM_LINUX, OTHER_LINUX -> startInstallOnLinux();
                            case MAC -> startInstallOnMac();
                            default -> throw new FBException("unsupported platform");
                        }
                    });
                }
        );
        downloadService.start();
    }

    private UpgradeConfig.ReleaseFileInfo getReleaseFileInfo() {
        UpgradeCheckResultBO upgradeCheckResult = getData();

        return upgradeCheckResult.getAvailableReleaseFileInfo();
    }

    private void startInstallOnWindows() {
        RuntimeUtil.exec("msiexec", "/i", "\"" + storagePath.getAbsolutePath() + "\"");
        exitApp();
    }

    private void startInstallOnLinux() {
        Alert alert = new Alert(
                Alert.AlertType.INFORMATION,
                I18nHelper.get(I18nKeys.UPGRADE_INSTALL_DIALOG_CONTENT),
                ButtonTypes.OPEN_PATH,
                ButtonTypes.OPEN_DIRECTLY
        );
        DialogPane dialogPane;

        alert.setTitle(I18nHelper.get(I18nKeys.UPGRADE_INSTALL_DIALOG_TITLE));
        alert.setHeaderText(I18nHelper.get(I18nKeys.UPGRADE_INSTALL_DIALOG_TITLE));
        dialogPane = alert.getDialogPane();
        WindowHelper.setFontFamily(dialogPane, ConfigHelper.getUsageFontFamily());
        dialogPane.lookupButton(ButtonTypes.OPEN_DIRECTLY)
                        .addEventFilter(
                                ActionEvent.ACTION,
                                evt -> {
                                    RuntimeUtil.exec("xdg-open", storagePath.getAbsolutePath());
                                    exitApp();
                                }
                        );
        dialogPane.lookupButton(ButtonTypes.OPEN_PATH)
                        .addEventFilter(
                                ActionEvent.ACTION,
                                evt -> {
                                    RuntimeUtil.exec("xdg-open", storagePath.getParent());
                                    exitApp();
                                }
                        );
        alert.show();
        WindowHelper.getStage(dialogPane).setWidth(500);
    }

    private void startInstallOnMac() {
        RuntimeUtil.exec("open", storagePath.getAbsolutePath());
        exitApp();
    }

    private void exitApp() {
        Platform.runLater(() -> {
            upgradingProperty.set(false);
            LoadingHelper.showLoading(
                    WindowHelper.getStage(root),
                    I18nKeys.MESSAGE_UPGRADE_QUIT_LOADING
            );
            Context.INSTANCE.destroy();
        });
    }
}
