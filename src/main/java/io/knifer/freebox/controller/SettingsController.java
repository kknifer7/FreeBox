package io.knifer.freebox.controller;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.helper.WindowHelper;
import io.knifer.freebox.util.NetworkUtil;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.BorderPane;
import org.apache.commons.lang3.tuple.Pair;

import java.net.NetworkInterface;
import java.util.Collection;

/**
 * 设置
 *
 * @author Knifer
 */
public class SettingsController {

    @FXML
    private BorderPane root;
    @FXML
    private ChoiceBox<Pair<NetworkInterface, String>> httpIpChoiceBox;

    @FXML
    private void initialize() {
        ConfigHelper.loadConfig();
        fillHttpIpChoiceBox();
    }

    private void fillHttpIpChoiceBox() {
        Collection<Pair<NetworkInterface, String>> networkInterfaceAndIps =
                NetworkUtil.getAvailableNetworkInterfaceAndIPv4();
        ObservableList<Pair<NetworkInterface, String>> items;

        if (networkInterfaceAndIps.isEmpty()) {
            ToastHelper.showError(I18nKeys.SETTINGS_FORM_HINT_NO_AVAILABLE_IP);

            return;
        }
        items = httpIpChoiceBox.getItems();
        items.addAll(networkInterfaceAndIps);
        httpIpChoiceBox.setValue(items.get(0));
    }

    @FXML
    private void onSaveBtnAction() {
        WindowHelper.close(root);
        ConfigHelper.saveConfig();
    }

    @FXML
    private void onCancelBtnAction() {
        WindowHelper.close(root);
    }
}
