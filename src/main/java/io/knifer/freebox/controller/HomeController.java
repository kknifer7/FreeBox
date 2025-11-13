package io.knifer.freebox.controller;

import io.knifer.freebox.component.node.IPInfoPopOver;
import io.knifer.freebox.component.node.ImportCatVodApiDialog;
import io.knifer.freebox.component.node.ImportSingleLiveApiDialog;
import io.knifer.freebox.component.node.ImportUrlApiDialog;
import io.knifer.freebox.constant.*;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.controller.dialog.LicenseDialogController;
import io.knifer.freebox.controller.dialog.UpgradeDialogController;
import io.knifer.freebox.exception.FBException;
import io.knifer.freebox.handler.VLCPlayerCheckHandler;
import io.knifer.freebox.handler.impl.LinuxVLCPlayerCheckHandler;
import io.knifer.freebox.handler.impl.MacVLCPlayerCheckHandler;
import io.knifer.freebox.handler.impl.WindowsRegistryVLCPlayerCheckHandler;
import io.knifer.freebox.helper.*;
import io.knifer.freebox.model.bo.UpgradeCheckResultBO;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.domain.MovieCollection;
import io.knifer.freebox.model.domain.MovieHistory;
import io.knifer.freebox.model.domain.SourceBeanBlockList;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.service.LoadNetworkInterfaceDataService;
import io.knifer.freebox.service.UpgradeCheckService;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.FXMLUtil;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.controlsfx.control.PopOver;
import org.controlsfx.dialog.CommandLinksDialog;

import java.net.NetworkInterface;
import java.util.Collection;
import java.util.function.Consumer;

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
    private HBox showIpPromptHBox;
    @FXML
    private HBox vlcHBox;
    @FXML
    private Label versionInfoLabel;
    @FXML
    private ListView<ClientInfo> clientListView;
    @FXML
    private Button vodButton;
    @FXML
    private Button liveButton;
    @FXML
    private Button sourceAuditButton;

    private PopOver ipInfoPopOver;

    private ClientManager clientManager;

    private final static VLCPlayerCheckHandler VLC_PLAYER_CHECK_HANDLER;

    static {
        switch (SystemHelper.getPlatform()) {
            case WINDOWS:
                VLC_PLAYER_CHECK_HANDLER = new WindowsRegistryVLCPlayerCheckHandler();
                break;
            case DEB_LINUX:
            case RPM_LINUX:
            case OTHER_LINUX:
                VLC_PLAYER_CHECK_HANDLER = new LinuxVLCPlayerCheckHandler();
                break;
            case MAC:
                VLC_PLAYER_CHECK_HANDLER = new MacVLCPlayerCheckHandler();
                break;
            default:
                throw new FBException("unsupported platform");
        }
    }

    @FXML
    private void initialize() {
        ObservableList<ClientInfo> clientItems = clientListView.getItems();
        boolean vlcNotInstalled = !VLC_PLAYER_CHECK_HANDLER.handle();

        vlcHBox.setVisible(vlcNotInstalled);
        vlcHBox.setManaged(vlcNotInstalled);
        clientListView.getSelectionModel()
                .selectedItemProperty()
                .addListener((ob, oldVal, newVal) -> {
                    // 选择不同的源时，根据源类型的不同，确定各功能按钮的可用性
                    ClientType clientType;

                    if (newVal == null || (clientType = newVal.getClientType()) == null) {
                        vodButton.setDisable(true);
                        liveButton.setDisable(true);
                        sourceAuditButton.setDisable(true);

                        return;
                    }
                    switch (clientType) {
                        case CATVOD_SPIDER:
                        case TVBOX_K:
                            vodButton.setDisable(false);
                            liveButton.setDisable(false);
                            sourceAuditButton.setDisable(false);
                            break;
                        case SINGLE_LIVE:
                            vodButton.setDisable(true);
                            liveButton.setDisable(false);
                            sourceAuditButton.setDisable(true);
                            break;
                    }
                });
        showIpPromptHBox.visibleProperty().bind(settingsInfoText.textProperty().isNotEmpty());
        showIpPromptHBox.managedProperty().bind(showIpPromptHBox.visibleProperty());
        ipInfoPopOver = new IPInfoPopOver();
        Context.INSTANCE.registerEventListener(AppEvents.APP_INITIALIZED, evt -> {
            clientManager = Context.INSTANCE.getClientManager();
            refreshServiceStatusInfo();
            StorageHelper.findAll(ClientInfo.class)
                    .values()
                    .stream()
                    .filter(c -> {
                        ClientType clientType = c.getClientType();

                        return clientType == ClientType.CATVOD_SPIDER || clientType == ClientType.SINGLE_LIVE;
                    })
                    .forEach(clientInfo -> {
                        clientManager.register(clientInfo);
                        clientItems.add(clientInfo);
                    });
            if (!clientItems.isEmpty()) {
                clientListView.getSelectionModel().selectFirst();
            }
            versionInfoLabel.setText(String.format(
                    I18nHelper.get(I18nKeys.HOME_VERSION_INFO),
                    ConfigHelper.getAppVersion()
            ));
            // 展示许可协议
            openLicenseDialogIfNeeded();
            // 自动检查更新
            openUpgradeDialogIfNeeded();
            initProgressIndicator.setVisible(false);
            settingsBtn.setDisable(false);
            root.setDisable(false);
        });
        Context.INSTANCE.registerEventListener(
                AppEvents.WsServerStartedEvent.class, evt -> refreshServiceStatusInfo()
        );
        Context.INSTANCE.registerEventListener(
                AppEvents.HttpServerStartedEvent.class, evt -> refreshServiceStatusInfo()
        );
        Context.INSTANCE.registerEventListener(AppEvents.ClientRegisteredEvent.class, evt -> {
            MultipleSelectionModel<ClientInfo> model = clientListView.getSelectionModel();
            ClientInfo clientInfo = evt.clientInfo();
            ClientInfo oldClientInfo;

            clientItems.removeIf(c -> c.getId().equals(clientInfo.getId()));
            clientItems.add(clientInfo);
            oldClientInfo = clientManager.getCurrentClientImmediately();
            if (
                    oldClientInfo != null &&
                    oldClientInfo.getId().equals(clientInfo.getId())
            ) {
                clientManager.updateCurrentClient(clientInfo);
            }
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

    private void openLicenseDialogIfNeeded() {
        Pair<Stage, LicenseDialogController> stageAndController;

        if (BooleanUtils.isFalse(ConfigHelper.getShowLicense())) {

            return;
        }
        stageAndController = FXMLUtil.loadDialog(Views.LICENSE_DIALOG);
        stageAndController.getLeft().showAndWait();
    }

    private void openUpgradeDialogIfNeeded() {
        Service<UpgradeCheckResultBO> service;

        if (!BooleanUtils.toBoolean(ConfigHelper.getAutoCheckUpgrade())) {

            return;
        }
        service = new UpgradeCheckService();
        service.setOnSucceeded(evt -> {
            UpgradeCheckResultBO result = service.getValue();
            Pair<Stage, UpgradeDialogController> stageAndController;

            if (result == null || !result.isHasNewVersion()) {

                return;
            }
            stageAndController = FXMLUtil.loadDialog(Views.UPGRADE_DIALOG);
            stageAndController.getRight().setData(result);
            stageAndController.getLeft().showAndWait();
        });
        service.start();
    }

    @FXML
    private void onSettingsBtnClick() {
        Stage stage = FXMLUtil.load(Views.SETTINGS).getLeft();

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
        String ip;
        LoadNetworkInterfaceDataService service;

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
        ip = ConfigHelper.getServiceIPv4();
        if (BaseValues.ANY_LOCAL_IP.equals(ip)) {
            service = new LoadNetworkInterfaceDataService();
            service.setOnSucceeded(evt -> {
                Collection<Pair<NetworkInterface, String>> value = service.getValue();

                settingsInfoText.setText(String.format(
                        I18nHelper.get(I18nKeys.HOME_SETTINGS_INFO),
                        CollectionUtil.isEmpty(value) ? ip : value.iterator().next().getValue(),
                        httpServiceRunningStatus,
                        httpPort,
                        wsServiceRunningStatus,
                        wsPort
                ));
            });
            service.start();
        } else {
            settingsInfoText.setText(String.format(
                    I18nHelper.get(I18nKeys.HOME_SETTINGS_INFO),
                    ObjectUtils.defaultIfNull(ip, "--"),
                    httpServiceRunningStatus,
                    httpPort,
                    wsServiceRunningStatus,
                    wsPort
            ));
        }

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
    private void onVodBtnAction() {
        ClientInfo clientInfo;

        if (!VLC_PLAYER_CHECK_HANDLER.handle()) {
            ToastHelper.showErrorAlert(
                    I18nKeys.HOME_MESSAGE_VLC_NOT_FOUND_TITLE,
                    I18nKeys.HOME_MESSAGE_VLC_NOT_FOUND,
                    evt -> {}
            );

            return;
        }
        clientInfo = clientListView.getSelectionModel().getSelectedItem();
        if (clientInfo == null) {
            return;
        }
        log.info("open client [{}]", clientInfo.getName());
        openVodClient(clientInfo);
    }

    private void openVodClient(ClientInfo clientInfo) {
        Stage homeStage = WindowHelper.getStage(root);
        Stage tvStage;
        Pair<Stage, TVController> stageAndController;

        clientManager.shutdownConnectingExecutor();
        clientManager.updateCurrentClient(clientInfo);
        stageAndController = FXMLUtil.load(Views.TV);
        tvStage = stageAndController.getLeft();
        WindowHelper.route(homeStage, tvStage);
    }

    @FXML
    private void onClientListCloseBtnAction() {
        ClientInfo clientInfo = clientListView.getSelectionModel().getSelectedItem();
        String clientId;

        if (clientInfo == null) {
            return;
        }
        clientManager.unregister(clientInfo);
        clientListView.getItems().remove(clientInfo);
        clientId = clientInfo.getId();
        StorageHelper.delete(clientId, SourceBeanBlockList.class);
        if (clientInfo.getClientType() == ClientType.CATVOD_SPIDER) {
            StorageHelper.delete(clientId, MovieHistory.class);
            StorageHelper.delete(clientId, MovieCollection.class);
            StorageHelper.delete(clientInfo);
            ToastHelper.showInfoI18n(
                    I18nKeys.HOME_MESSAGE_REMOVE_SPIDER_CONFIG_SUCCEED,
                    clientInfo.getName()
            );

        } else if (clientInfo.getClientType() == ClientType.SINGLE_LIVE) {
            StorageHelper.delete(clientInfo);
            ToastHelper.showInfoI18n(
                    I18nKeys.HOME_MESSAGE_REMOVE_SPIDER_CONFIG_SUCCEED,
                    clientInfo.getName()
            );
        } else {
            ToastHelper.showInfoI18n(
                    I18nKeys.MESSAGE_CLIENT_UNREGISTERED,
                    clientInfo.getName()
            );
        }
    }

    @FXML
    private void onSourceAuditBtnAction() {
        ClientInfo clientInfo = clientListView.getSelectionModel().getSelectedItem();
        Pair<Stage, SourceAuditController> stageAndController;
        Stage homeStage;
        Stage sourceAuditStage;

        if (clientInfo == null) {
            return;
        }
        clientManager.shutdownConnectingExecutor();
        clientManager.updateCurrentClient(clientInfo);
        stageAndController = FXMLUtil.load(Views.SOURCE_AUDIT);
        homeStage = WindowHelper.getStage(root);
        sourceAuditStage = stageAndController.getLeft();
        log.info("enter source audit for [{}]", clientInfo.getName());
        WindowHelper.route(homeStage, sourceAuditStage);
    }

    @FXML
    private void onVLCDownloadHyperlinkClick() {
        if (SystemHelper.getPlatform() == Platform.WINDOWS) {
            HostServiceHelper.showDocument(BaseValues.VLC_DOWNLOAD_URL_WINDOWS);
        } else {
            HostServiceHelper.showDocument(BaseValues.VLC_DOWNLOAD_URL);
        }
    }

    @FXML
    private void onImportApiBtnAction() {
        CommandLinksDialog importApiSelectTypeDialog = new CommandLinksDialog(
                ButtonTypes.CAT_VOD_COMMAND_LINKS_BUTTON_TYPE,
                ButtonTypes.SINGLE_LIVE_COMMAND_LINKS_BUTTON_TYPE,
                ButtonTypes.CANCEL_COMMAND_LINKS_BUTTON_TYPE
        );
        Consumer<ClientInfo> clientInfoConsumer = clientInfo -> {
            StorageHelper.save(clientInfo);
            if (!clientManager.isRegistered(clientInfo)) {
                clientManager.register(clientInfo);
                Context.INSTANCE.postEvent(new AppEvents.ClientRegisteredEvent(clientInfo));
            }
        };
        ButtonType result;
        ImportUrlApiDialog importDialog;

        importApiSelectTypeDialog.setTitle(I18nHelper.get(I18nKeys.HOME_IMPORT_API_SELECT_TYPE_TITLE));
        importApiSelectTypeDialog.setContentText(I18nHelper.get(I18nKeys.HOME_IMPORT_API_SELECT_TYPE_CONTENT));
        WindowHelper.setFontFamily(importApiSelectTypeDialog.getDialogPane(), ConfigHelper.getUsageFontFamily());
        importApiSelectTypeDialog.showAndWait();
        result = importApiSelectTypeDialog.getResult();
        if (result == ButtonTypes.CAT_VOD_COMMAND_LINKS_BUTTON_TYPE.getButtonType()) {
            importDialog = new ImportCatVodApiDialog(clientInfoConsumer);
        } else if (result == ButtonTypes.SINGLE_LIVE_COMMAND_LINKS_BUTTON_TYPE.getButtonType()) {
            importDialog = new ImportSingleLiveApiDialog(clientInfoConsumer);
        } else {

            return;
        }
        importDialog.showAndWait();
        if (BaseValues.SUCCESS_STR.equals(importDialog.getResult())) {
            ToastHelper.showSuccessI18n(I18nKeys.HOME_IMPORT_API_MESSAGE_SAVE_CONFIG_SUCCEED);
        }
    }

    @FXML
    private void onLiveBtnAction() {
        ClientInfo clientInfo = clientListView.getSelectionModel().getSelectedItem();
        Pair<Stage, LiveController> stageAndController;
        Stage homeStage;
        Stage liveStage;

        if (clientInfo == null) {
            return;
        }
        clientManager.shutdownConnectingExecutor();
        clientManager.updateCurrentClient(clientInfo);
        stageAndController = FXMLUtil.load(Views.LIVE);
        homeStage = WindowHelper.getStage(root);
        liveStage = stageAndController.getLeft();
        liveStage.setTitle(I18nHelper.getFormatted(I18nKeys.LIVE_WINDOW_TITLE, clientInfo.getName()));
        stageAndController.getRight().setData(clientInfo);
        WindowHelper.route(homeStage, liveStage);
    }

    @FXML
    private void onShowAllIpHyperlinkAction(ActionEvent event) {
        Hyperlink hyperlink = (Hyperlink) event.getSource();

        hyperlink.setVisited(false);
        ipInfoPopOver.show(hyperlink);

    }
}