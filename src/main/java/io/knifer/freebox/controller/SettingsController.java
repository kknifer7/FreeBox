package io.knifer.freebox.controller;

import io.knifer.freebox.component.validator.PortValidator;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.*;
import io.knifer.freebox.net.http.FreeBoxHttpServerHolder;
import io.knifer.freebox.net.websocket.FreeBoxWebSocketServerHolder;
import io.knifer.freebox.service.LoadConfigService;
import io.knifer.freebox.service.LoadNetworkInterfaceDataService;
import io.knifer.freebox.util.NetworkUtil;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
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

    private final ObjectProperty<Pair<NetworkInterface, String>> ipValueProp = new SimpleObjectProperty<>();
    private final BooleanProperty ipChoiceBoxDisableProp = new SimpleBooleanProperty();

    private final LoadConfigService loadConfigService = new LoadConfigService();
    private final ValidationSupport validationSupport = new ValidationSupport();

    private final FreeBoxHttpServerHolder httpServer = Context.INSTANCE.getHttpServer();
    private final FreeBoxWebSocketServerHolder wsServer = Context.INSTANCE.getWsServer();

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
            httpServiceStatusLabel.setText(I18nHelper.get(I18nKeys.SETTINGS_SERVICE_UP));
            httpServiceStatusFontIcon.setIconColor(Color.GREEN);
            httpServiceStartBtn.setDisable(true);
            disableHttpServiceForm();
        } else {
            httpServiceStatusLabel.setText(I18nHelper.get(I18nKeys.SETTINGS_SERVICE_DOWN));
            httpServiceStatusFontIcon.setIconColor(Color.GRAY);
            httpServiceStopBtn.setDisable(true);
        }
        if (wsServer.isRunning()) {
            wsServiceStatusLabel.setText(I18nHelper.get(I18nKeys.SETTINGS_SERVICE_UP));
            wsServiceStatusFontIcon.setIconColor(Color.GREEN);
            wsServiceStartBtn.setDisable(true);
            disableWsServiceForm();
        } else {
            wsServiceStatusLabel.setText(I18nHelper.get(I18nKeys.SETTINGS_SERVICE_DOWN));
            wsServiceStatusFontIcon.setIconColor(Color.GRAY);
            wsServiceStopBtn.setDisable(true);
        }
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

        disableHttpServiceBtn();
        if (NetworkUtil.isPortUsing(httpPort)) {
            ToastHelper.showError(String.format(
                    I18nHelper.get(I18nKeys.SETTINGS_PORT_IN_USE),
                    httpPort
            ));
            httpServiceStartBtn.setDisable(false);

            return;
        }
        ConfigHelper.checkAndSave();
        httpServer.start(ConfigHelper.getServiceIPv4(), httpPort);
        httpServiceStatusLabel.setText(I18nHelper.get(I18nKeys.SETTINGS_SERVICE_UP));
        httpServiceStatusFontIcon.setIconColor(Color.GREEN);
        httpServiceStopBtn.setDisable(false);
        disableHttpServiceForm();
        ToastHelper.showSuccessI18n(I18nKeys.SETTINGS_HTTP_SERVICE_UP);
    }

    @FXML
    public void onHttpServiceStopBtnAction() {
        disableHttpServiceBtn();
        httpServer.stop(() -> {
            httpServiceStatusLabel.setText(I18nHelper.get(I18nKeys.SETTINGS_SERVICE_DOWN));
            httpServiceStatusFontIcon.setIconColor(Color.GRAY);
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

        disableWsServiceBtn();
        if (NetworkUtil.isPortUsing(wsPort)) {
            ToastHelper.showError(String.format(
                    I18nHelper.get(I18nKeys.SETTINGS_PORT_IN_USE),
                    wsPort
            ));
            wsServiceStartBtn.setDisable(false);

            return;
        }
        ConfigHelper.checkAndSave();
        wsServer.start(ConfigHelper.getServiceIPv4(), wsPort);
        wsServiceStatusLabel.setText(I18nHelper.get(I18nKeys.SETTINGS_SERVICE_UP));
        wsServiceStatusFontIcon.setIconColor(Color.GREEN);
        wsServiceStopBtn.setDisable(false);
        disableWsServiceForm();
        ToastHelper.showSuccessI18n(I18nKeys.SETTINGS_WS_SERVICE_UP);
    }

    @FXML
    private void onWsServiceStopBtnAction() {
        disableWsServiceBtn();
        wsServer.stop(() -> {
            wsServiceStatusLabel.setText(I18nHelper.get(I18nKeys.SETTINGS_SERVICE_DOWN));
            wsServiceStatusFontIcon.setIconColor(Color.GRAY);
            wsServiceStartBtn.setDisable(false);
            enableWsServiceForm();
            ToastHelper.showSuccessI18n(I18nKeys.SETTINGS_WS_SERVICE_DOWN);
        });
    }

    private void disableWsServiceBtn() {
        wsServiceStartBtn.setDisable(true);
        wsServiceStopBtn.setDisable(true);
    }
}
