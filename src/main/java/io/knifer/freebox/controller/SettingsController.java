package io.knifer.freebox.controller;

import cn.hutool.core.io.FileUtil;
import io.knifer.freebox.component.validator.PortValidator;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.Views;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.controller.dialog.UpgradeDialogController;
import io.knifer.freebox.helper.*;
import io.knifer.freebox.model.bo.UpgradeCheckResultBO;
import io.knifer.freebox.net.http.server.FreeBoxHttpServerHolder;
import io.knifer.freebox.net.websocket.server.KebSocketServerHolder;
import io.knifer.freebox.service.CheckPortUsingService;
import io.knifer.freebox.service.LoadConfigService;
import io.knifer.freebox.service.LoadNetworkInterfaceDataService;
import io.knifer.freebox.service.UpgradeCheckService;
import io.knifer.freebox.util.CastUtil;
import io.knifer.freebox.util.FXMLUtil;
import io.knifer.freebox.util.FormattingUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.controlsfx.validation.ValidationSupport;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.NetworkInterface;
import java.util.Collection;
import java.util.Objects;




/**
 * 设置
 *
 * @author Knifer
 */
@Slf4j
public class SettingsController {

    @FXML
    private BorderPane root;
    @FXML
    private Button saveBtn;
    @FXML
    private HBox networkAndServiceHBox;
    @FXML
    private ChoiceBox<Pair<NetworkInterface, String>> httpIpChoiceBox;
    @FXML
    private ProgressIndicator loadingProgressIndicator;
    @FXML
    private TextField httpPortTextField;
    @FXML
    private CheckBox httpAutoStartCheckBox;
    @FXML
    private Label httpServiceStatusLabel;
    @FXML
    private FontIcon httpServiceStatusFontIcon;
    @FXML
    private Button httpServiceStartBtn;
    @FXML
    public Button httpServiceStopBtn;
    @FXML
    private ChoiceBox<Pair<NetworkInterface, String>> wsIpChoiceBox;
    @FXML
    public Label wsServiceStatusLabel;
    @FXML
    public FontIcon wsServiceStatusFontIcon;
    @FXML
    private Button wsServiceStartBtn;
    @FXML
    public Button wsServiceStopBtn;
    @FXML
    private TextField wsPortTextField;
    @FXML
    private CheckBox wsAutoStartCheckBox;
    @FXML
    private Label applicationDataLabel;
    @FXML
    private Label applicationVersionLabel;
    @FXML
    private Button checkUpgradeButton;
    @FXML
    private Label alreadyLatestVersionLabel;

    private final ObjectProperty<Pair<NetworkInterface, String>> ipValueProp = new SimpleObjectProperty<>();
    private final BooleanProperty ipChoiceBoxDisableProp = new SimpleBooleanProperty();

    private final LoadConfigService loadConfigService = new LoadConfigService();
    private final ValidationSupport validationSupport = new ValidationSupport();

    private final FreeBoxHttpServerHolder httpServer = Context.INSTANCE.getHttpServer();
    private final KebSocketServerHolder wsServer = Context.INSTANCE.getWsServer();

    @FXML
    private void initialize() {
        LoadNetworkInterfaceDataService loadNetworkInterfaceService = new LoadNetworkInterfaceDataService();

        loadConfigService.setOnSucceeded(evt -> loadNetworkInterfaceService.restart());
        loadNetworkInterfaceService.setOnSucceeded(evt -> {
            // 网卡信息获取完成，填充数据
            putDataInForm(loadNetworkInterfaceService.getValue());
            setupComponent();
            loadingProgressIndicator.setVisible(false);
            networkAndServiceHBox.setVisible(true);
            saveBtn.setDisable(false);
        });
        loadConfigService.start();
    }

    private void putDataInForm(
            Collection<Pair<NetworkInterface, String>> networkInterfaceAndIps
    ) {
        putDataInIpChoiceBox(networkInterfaceAndIps);
        putDataInOtherComponent();
    }

    private void putDataInIpChoiceBox(
            Collection<Pair<NetworkInterface, String>> networkInterfaceAndIps
    ) {
        ObservableList<Pair<NetworkInterface, String>> items;
        String configIP;

        if (networkInterfaceAndIps.isEmpty()) {
            ToastHelper.showErrorI18n(I18nKeys.SETTINGS_FORM_HINT_NO_AVAILABLE_IP);

            return;
        }
        items = httpIpChoiceBox.getItems();
        wsIpChoiceBox.setItems(items);
        items.clear();
        items.add(Pair.of(null, BaseValues.ANY_LOCAL_IP));
        items.addAll(networkInterfaceAndIps);
        configIP = ConfigHelper.getServiceIPv4();
        if (StringUtils.isBlank(configIP)) {
            // 如果没有找到 网卡-IP 配置，填充可用列表中的第一个
            ipValueProp.setValue(items.get(0));
        } else {
            // 如果配置了 网卡-IP，尝试填充
            for (Pair<NetworkInterface, String> item : items) {
                if (Objects.equals(item.getRight(), configIP)) {
                    ipValueProp.setValue(item);

                    return;
                }
            }
            // 网络环境发生了改变，自动填充第一个配置，并标记配置更新
            ipValueProp.setValue(items.get(0));
            ConfigHelper.markToUpdate();
        }
    }

    private void putDataInOtherComponent() {
        Integer configPort = ConfigHelper.getHttpPort();

        if (configPort != null) {
            httpPortTextField.setText(configPort.toString());
        }
        configPort = ConfigHelper.getWsPort();
        if (configPort != null) {
            wsPortTextField.setText(configPort.toString());
        }
        httpAutoStartCheckBox.setSelected(BooleanUtils.toBoolean(ConfigHelper.getAutoStartHttp()));
        wsAutoStartCheckBox.setSelected(BooleanUtils.toBoolean(ConfigHelper.getAutoStartWs()));
    }

    private void setupComponent() {
        String applicationDataSize;

        // 注册表单验证器
        validationSupport.registerValidator(httpPortTextField, PortValidator.getInstance());
        validationSupport.registerValidator(wsPortTextField, PortValidator.getInstance());

        // 表单数据监听与绑定
        httpPortTextField.textProperty().addListener((ob, oldVal, newVal) -> onHttpPortTextFieldChange());
        wsPortTextField.textProperty().addListener((ob, oldVal, newVal) -> onWsPortTextFieldChange());
        httpIpChoiceBox.valueProperty().bindBidirectional(ipValueProp);
        wsIpChoiceBox.valueProperty().bindBidirectional(ipValueProp);
        httpIpChoiceBox.disableProperty().bind(ipChoiceBoxDisableProp);
        wsIpChoiceBox.disableProperty().bind(ipChoiceBoxDisableProp);

        // 服务状态显示
        if (httpServer.isRunning()) {
            showServiceStatus(
                    httpServiceStatusLabel,
                    httpServiceStatusFontIcon,
                    Color.GREEN
            );
            httpServiceStartBtn.setDisable(true);
            disableHttpServiceForm();
        } else {
            showServiceStatus(
                    httpServiceStatusLabel,
                    httpServiceStatusFontIcon,
                    Color.GRAY
            );
            httpServiceStopBtn.setDisable(true);
        }
        if (wsServer.isRunning()) {
            showServiceStatus(
                    wsServiceStatusLabel,
                    wsServiceStatusFontIcon,
                    Color.GREEN
            );
            wsServiceStartBtn.setDisable(true);
            disableWsServiceForm();
        } else {
            showServiceStatus(
                    wsServiceStatusLabel,
                    wsServiceStatusFontIcon,
                    Color.GRAY
            );
            wsServiceStopBtn.setDisable(true);
        }

        // 常规设置tab
        applicationDataSize = FormattingUtil.sizeFormat(FileUtil.size(StorageHelper.getLocalStoragePath().toFile()));
        applicationDataLabel.setText(I18nHelper.getFormatted(I18nKeys.SETTINGS_APPLICATION_DATA, applicationDataSize));
        applicationVersionLabel.setText(
                I18nHelper.getFormatted(I18nKeys.SETTINGS_APPLICATION_VERSION, ConfigHelper.getAppVersion())
        );
    }

    private void disableHttpServiceForm() {
        ipChoiceBoxDisableProp.set(true);
        httpAutoStartCheckBox.setDisable(true);
        httpPortTextField.setDisable(true);
    }

    private void enableHttpServiceForm() {
        if (!wsServer.isRunning()) {
            ipChoiceBoxDisableProp.set(false);
        }
        httpAutoStartCheckBox.setDisable(false);
        httpPortTextField.setDisable(false);
    }

    private void disableWsServiceForm() {
        ipChoiceBoxDisableProp.set(true);
        wsAutoStartCheckBox.setDisable(true);
        wsPortTextField.setDisable(true);
    }

    private void enableWsServiceForm() {
        if (!httpServer.isRunning()) {
            ipChoiceBoxDisableProp.set(false);
        }
        wsAutoStartCheckBox.setDisable(false);
        wsPortTextField.setDisable(false);
    }

    private void onHttpPortTextFieldChange() {
        // 延迟执行，等待Validator验证结束
        Platform.runLater(() -> {
            int newPort;

            if (!ValidationHelper.validate(validationSupport, httpPortTextField)) {
                return;
            }
            newPort = Integer.parseInt(httpPortTextField.getText());
            if (Objects.equals(ConfigHelper.getHttpPort(), newPort)) {
                return;
            }
            ConfigHelper.setHttpPort(newPort);
            ConfigHelper.markToUpdate();
        });
    }

    @FXML
    private void onSaveBtnAction() {
        if (ValidationHelper.validate(validationSupport)) {
            WindowHelper.close(root);
            ConfigHelper.checkAndSave();
        }
    }

    @FXML
    private void onCancelBtnAction() {
        WindowHelper.close(root);
        ConfigHelper.unmarkToUpdate();
        loadConfigService.restart();
    }

    @FXML
    @SuppressWarnings("unchecked")
    private void onIpChoiceBoxAction(ActionEvent event) {
        ChoiceBox<Pair<NetworkInterface, String>> ipChoiceBox =
                (ChoiceBox<Pair<NetworkInterface, String>>) event.getTarget();
        Pair<NetworkInterface, String> value = ipChoiceBox.getValue();
        String newIP;

        if (value == null) {
            return;
        }
        newIP = value.getRight();
        if (Objects.equals(ConfigHelper.getServiceIPv4(), newIP)) {
            return;
        }
        ConfigHelper.setServiceIPv4(value.getRight());
        ConfigHelper.markToUpdate();
    }

    @FXML
    private void onHttpAutoStartCheckBoxAction() {
        boolean autoStartHttp = httpAutoStartCheckBox.isSelected();

        if (Objects.equals(ConfigHelper.getAutoStartHttp(), autoStartHttp)) {
            return;
        }
        ConfigHelper.setAutoStartHttp(autoStartHttp);
        ConfigHelper.markToUpdate();
    }

    @FXML
    public void onHttpServiceStartBtnAction() {
        Integer httpPort = ConfigHelper.getHttpPort();
        String ip = ConfigHelper.getServiceIPv4();
        CheckPortUsingService checkPortUsingService = new CheckPortUsingService(ip, httpPort);

        disableHttpServiceBtn();
        disableHttpServiceForm();
        showServiceStatus(
                httpServiceStatusLabel,
                httpServiceStatusFontIcon,
                Color.ORANGE
        );
        checkPortUsingService.setOnSucceeded(evt -> {
            if (checkPortUsingService.getValue()) {
                ToastHelper.showError(String.format(
                        I18nHelper.get(I18nKeys.SETTINGS_PORT_IN_USE),
                        httpPort
                ));
                showServiceStatus(
                        httpServiceStatusLabel,
                        httpServiceStatusFontIcon,
                        Color.GRAY
                );
                httpServiceStartBtn.setDisable(false);
                enableHttpServiceForm();
            } else {
                ConfigHelper.checkAndSave();
                httpServer.start(ip, httpPort);
                showServiceStatus(
                        httpServiceStatusLabel,
                        httpServiceStatusFontIcon,
                        Color.GREEN
                );
                httpServiceStopBtn.setDisable(false);
                ToastHelper.showSuccessI18n(I18nKeys.SETTINGS_HTTP_SERVICE_UP);
            }
        });
        checkPortUsingService.start();
    }

    @FXML
    public void onHttpServiceStopBtnAction() {
        disableHttpServiceBtn();
        httpServer.stop(() -> {
            showServiceStatus(
                    httpServiceStatusLabel,
                    httpServiceStatusFontIcon,
                    Color.GRAY
            );
            httpServiceStartBtn.setDisable(false);
            enableHttpServiceForm();
            ToastHelper.showSuccessI18n(I18nKeys.SETTINGS_HTTP_SERVICE_DOWN);
        });
    }

    private void disableHttpServiceBtn() {
        httpServiceStartBtn.setDisable(true);
        httpServiceStopBtn.setDisable(true);
    }

    @FXML
    private void onWsAutoStartCheckBoxAction() {
        boolean autoStartWs = wsAutoStartCheckBox.isSelected();

        if (Objects.equals(ConfigHelper.getAutoStartWs(), autoStartWs)) {
            return;
        }
        ConfigHelper.setAutoStartWs(autoStartWs);
        ConfigHelper.markToUpdate();
    }

    private void onWsPortTextFieldChange() {
        Platform.runLater(() -> {
            int newPort;

            if (!ValidationHelper.validate(validationSupport, wsPortTextField)) {
                return;
            }
            newPort = Integer.parseInt(wsPortTextField.getText());
            if (Objects.equals(ConfigHelper.getWsPort(), newPort)) {
                return;
            }
            ConfigHelper.setWsPort(newPort);
            ConfigHelper.markToUpdate();
        });
    }

    @FXML
    private void onWsServiceStartBtnAction() {
        Integer wsPort = ConfigHelper.getWsPort();
        String ip = ConfigHelper.getServiceIPv4();
        CheckPortUsingService checkPortUsingService = new CheckPortUsingService(ip, wsPort);

        disableWsServiceBtn();
        disableWsServiceForm();
        showServiceStatus(
                wsServiceStatusLabel,
                wsServiceStatusFontIcon,
                Color.ORANGE
        );
        checkPortUsingService.setOnSucceeded(evt -> {
            if (checkPortUsingService.getValue()) {
                ToastHelper.showError(String.format(
                        I18nHelper.get(I18nKeys.SETTINGS_PORT_IN_USE),
                        wsPort
                ));
                showServiceStatus(
                        wsServiceStatusLabel,
                        wsServiceStatusFontIcon,
                        Color.GRAY
                );
                wsServiceStartBtn.setDisable(false);
                enableWsServiceForm();
            } else {
                ConfigHelper.checkAndSave();
                wsServer.start(ip, wsPort);
                showServiceStatus(
                        wsServiceStatusLabel,
                        wsServiceStatusFontIcon,
                        Color.GREEN
                );
                wsServiceStopBtn.setDisable(false);
                ToastHelper.showSuccessI18n(I18nKeys.SETTINGS_WS_SERVICE_UP);
            }
        });
        checkPortUsingService.start();
    }

    private void showServiceStatus(
            Label label,
            FontIcon icon,
            Color color
    ) {
        if (color == Color.GREEN) {
            label.setText(I18nHelper.get(I18nKeys.SETTINGS_SERVICE_UP));
        } else if (color == Color.ORANGE) {
            label.setText(I18nHelper.get(I18nKeys.SETTINGS_SERVICE_STARTING));
        } else if (color == Color.GRAY) {
            label.setText(I18nHelper.get(I18nKeys.SETTINGS_SERVICE_DOWN));
        }
        icon.setIconColor(color);
    }

    @FXML
    private void onWsServiceStopBtnAction() {
        disableWsServiceBtn();
        wsServer.stop(() -> {
            showServiceStatus(
                    wsServiceStatusLabel,
                    wsServiceStatusFontIcon,
                    Color.GRAY
            );
            wsServiceStartBtn.setDisable(false);
            enableWsServiceForm();
            ToastHelper.showSuccessI18n(I18nKeys.SETTINGS_WS_SERVICE_DOWN);
        });
    }

    private void disableWsServiceBtn() {
        wsServiceStartBtn.setDisable(true);
        wsServiceStopBtn.setDisable(true);
    }

    @FXML
    private void onDeleteApplicationDataButtonAction() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        Button okBtn;

        alert.setContentText(I18nHelper.get(I18nKeys.SETTINGS_DELETE_APPLICATION_DATA_ALERT));
        alert.getButtonTypes().add(ButtonType.CLOSE);
        okBtn = CastUtil.cast(alert.getDialogPane().lookupButton(ButtonType.OK));
        okBtn.setOnAction(evt -> {
            LoadingHelper.showLoading(WindowHelper.getStage(root), I18nKeys.MESSAGE_QUIT_LOADING);
            StorageHelper.clearData();
            Context.INSTANCE.destroy();
        });
        alert.show();
    }

    @FXML
    private void onCheckUpgradeButtonAction() {
        Service<UpgradeCheckResultBO> service;

        checkUpgradeButton.setDisable(true);
        if (alreadyLatestVersionLabel.isVisible()) {
            alreadyLatestVersionLabel.setVisible(false);
        }
        service = new UpgradeCheckService();
        service.setOnSucceeded(evt -> {
            UpgradeCheckResultBO upgradeCheckResult = service.getValue();
            Pair<Stage, UpgradeDialogController> stageAndController;

            log.info("Upgrade check result: {}", upgradeCheckResult);
            checkUpgradeButton.setDisable(false);
            if (!upgradeCheckResult.isHasNewVersion()) {
                alreadyLatestVersionLabel.setVisible(true);

                return;
            }
            stageAndController = FXMLUtil.loadDialog(Views.UPGRADE_DIALOG);
            stageAndController.getRight().setData(upgradeCheckResult);
            stageAndController.getLeft().showAndWait();
        });
        service.start();
    }
}
