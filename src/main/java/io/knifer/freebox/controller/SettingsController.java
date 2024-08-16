package io.knifer.freebox.controller;

import io.knifer.freebox.component.validator.PortValidator;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.*;
import io.knifer.freebox.net.http.FreeBoxHttpServer;
import io.knifer.freebox.service.LoadConfigService;
import io.knifer.freebox.service.LoadNetworkInterfaceDataService;
import io.knifer.freebox.util.NetworkUtil;
import javafx.application.Platform;
import javafx.collections.ObservableList;
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

    private final LoadConfigService loadConfigService = new LoadConfigService();
    private final ValidationSupport validationSupport = new ValidationSupport();

    private final FreeBoxHttpServer httpServer = Context.INSTANCE.getHttpServer();

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
        putDataInHttpIpChoiceBox(networkInterfaceAndIps);
        putDataInOtherComponent();
    }

    private void putDataInHttpIpChoiceBox(
            Collection<Pair<NetworkInterface, String>> networkInterfaceAndIps
    ) {
        ObservableList<Pair<NetworkInterface, String>> items;
        String configIP;

        if (networkInterfaceAndIps.isEmpty()) {
            ToastHelper.showErrorI18n(I18nKeys.SETTINGS_FORM_HINT_NO_AVAILABLE_IP);

            return;
        }
        items = httpIpChoiceBox.getItems();
        items.clear();
        items.addAll(networkInterfaceAndIps);
        configIP = ConfigHelper.getServiceIPv4();
        if (StringUtils.isBlank(configIP)) {
            // 如果没有找到 网卡-IP 配置，填充可用列表中的第一个
            httpIpChoiceBox.setValue(items.get(0));
        } else {
            // 如果配置了 网卡-IP，尝试填充
            for (Pair<NetworkInterface, String> item : items) {
                if (Objects.equals(item.getRight(), configIP)) {
                    httpIpChoiceBox.setValue(item);
                    break;
                }
            }
        }
    }

    private void putDataInOtherComponent() {
        Integer configPort = ConfigHelper.getHttpPort();

        if (configPort != null) {
            httpPortTextField.setText(configPort.toString());
        }
        httpAutoStartCheckBox.setSelected(BooleanUtils.toBoolean(ConfigHelper.getAutoStartHttp()));
    }

    private void setupComponent() {
        validationSupport.registerValidator(httpPortTextField, PortValidator.getInstance());
        httpPortTextField.textProperty().addListener((ob, oldVal, newVal) -> onHttpPortTextFieldChange());

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
    }

    private void disableHttpServiceForm() {
        httpIpChoiceBox.setDisable(true);
        httpAutoStartCheckBox.setDisable(true);
        httpPortTextField.setDisable(true);
    }

    private void enableHttpServiceForm() {
        httpIpChoiceBox.setDisable(false);
        httpAutoStartCheckBox.setDisable(false);
        httpPortTextField.setDisable(false);
    }

    @FXML
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
    private void onHttpIpChoiceBoxAction() {
        Pair<NetworkInterface, String> value = httpIpChoiceBox.getValue();
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
}
