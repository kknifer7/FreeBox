package io.knifer.freebox.controller;

import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.Views;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.*;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.util.FXMLUtil;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;

@Slf4j
public class HomeController {

    @FXML
    private BorderPane root;
    @FXML
    private ProgressIndicator initProgressIndicator;
    @FXML
    private Button settingsBtn;
    @FXML
    private Text settingsInfoText;
    @FXML
    public ListView<ClientInfo> clientListView;

    @FXML
    private void initialize() {
        ObservableList<ClientInfo> clientItems = clientListView.getItems();

        Context.INSTANCE.registerEventListener(AppEvents.APP_INITIALIZED, evt -> {
            refreshServiceStatusInfo();
            initProgressIndicator.setVisible(false);
            settingsBtn.setDisable(false);
            root.setDisable(false);
        });
        Context.INSTANCE.registerEventListener(AppEvents.ClientRegisteredEvent.class, evt -> {
            MultipleSelectionModel<ClientInfo> model = clientListView.getSelectionModel();
            ClientInfo clientInfo = evt.clientInfo();

            clientItems.remove(clientInfo);
            clientItems.add(clientInfo);
            if (model.getSelectedItem() == null) {
                model.select(clientInfo);
            }
        });
        Context.INSTANCE.registerEventListener(AppEvents.ClientUnregisteredEvent.class, evt -> {
            MultipleSelectionModel<ClientInfo> model = clientListView.getSelectionModel();

            clientItems.remove(evt.clientInfo());
            if (model.getSelectedItem() == null && !clientItems.isEmpty()) {
                model.selectFirst();
            }
        });
    }

    @FXML
    private void onSettingsBtnClick() {
        Stage stage = FXMLUtil.load(Views.SETTINGS).getLeft();

        stage.initStyle(StageStyle.UTILITY);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle(I18nHelper.get(I18nKeys.SETTINGS));
        stage.showAndWait();
        refreshServiceStatusInfo();
    }

    private void refreshServiceStatusInfo() {
        boolean isHttpServiceRunning = Context.INSTANCE.getHttpServer().isRunning();
        boolean isWsServiceRunning = Context.INSTANCE.getWsServer().isRunning();
        String httpServiceRunningStatus;
        String httpPort;
        String wsServiceRunningStatus;
        String wsPort;

        if (isHttpServiceRunning) {
            httpServiceRunningStatus = I18nHelper.get(I18nKeys.SETTINGS_SERVICE_UP);
            httpPort = ConfigHelper.getHttpPort().toString();
        } else {
            httpServiceRunningStatus = I18nHelper.get(I18nKeys.SETTINGS_SERVICE_DOWN);
            httpPort = "--";
        }
        if (isWsServiceRunning) {
            wsServiceRunningStatus = I18nHelper.get(I18nKeys.SETTINGS_SERVICE_UP);
            wsPort = ConfigHelper.getWsPort().toString();
        } else {
            wsServiceRunningStatus = I18nHelper.get(I18nKeys.SETTINGS_SERVICE_DOWN);
            wsPort = "--";
        }
        settingsInfoText.setText(String.format(
                I18nHelper.get(I18nKeys.HOME_SETTINGS_INFO),
                ObjectUtils.defaultIfNull(ConfigHelper.getServiceIPv4(), "--"),
                httpServiceRunningStatus,
                httpPort,
                wsServiceRunningStatus,
                wsPort
        ));
    }

    @FXML
    public void onFreeBoxRepositoryHyperLinkClick() {
        HostServiceHelper.showDocument(BaseValues.REPOSITORY_URL);
    }

    @FXML
    private void onTvBoxKRepositoryHyperLinkClick() {
        HostServiceHelper.showDocument(BaseValues.TV_BOX_K_REPOSITORY_URL);
    }

    @FXML
    private void onExitBtnClick(ActionEvent event) {
        WindowHelper.close(event);
    }

    @FXML
    private void onClientListChooseBtnAction() {
        ClientInfo clientInfo = clientListView.getSelectionModel().getSelectedItem();

        if (clientInfo == null || !clientInfo.getConnection().isOpen()) {
            return;
        }
        log.info("open client [{}]", clientInfo.getConnection().getRemoteSocketAddress().getHostString());
        openClient(clientInfo);
    }

    private void openClient(ClientInfo clientInfo) {
        Pair<Stage, TVController> stageAndController = FXMLUtil.load(Views.TV);
        Stage homeStage = WindowHelper.getStage(root);
        Stage tvStage = stageAndController.getLeft();

        stageAndController.getRight().setData(clientInfo);
        WindowHelper.route(homeStage, tvStage);
    }

    @FXML
    private void onClientListCloseBtnAction() {
        ClientInfo clientInfo = clientListView.getSelectionModel().getSelectedItem();

        if (clientInfo == null) {
            return;
        }
        Context.INSTANCE.getClientManager().unregister(clientInfo);
        ToastHelper.showInfoI18n(
                I18nKeys.MESSAGE_CLIENT_UNREGISTERED,
                clientInfo.getConnection().getRemoteSocketAddress().getHostName()
        );
        clientListView.getItems().remove(clientInfo);
    }

    @FXML
    private void onSourceAuditBtnAction() {
        ClientInfo clientInfo = clientListView.getSelectionModel().getSelectedItem();
        Pair<Stage, SourceAuditController> stageAndController;
        Stage homeStage;
        Stage sourceAuditStage;

        if (clientInfo == null || !clientInfo.getConnection().isOpen()) {
            return;
        }
        stageAndController = FXMLUtil.load(Views.SOURCE_AUDIT);
        homeStage = WindowHelper.getStage(root);
        sourceAuditStage = stageAndController.getLeft();
        stageAndController.getRight().setData(clientInfo);
        log.info("enter source audit for [{}]", clientInfo.getConnection().getRemoteSocketAddress().getHostName());
        WindowHelper.route(homeStage, sourceAuditStage);
    }
}