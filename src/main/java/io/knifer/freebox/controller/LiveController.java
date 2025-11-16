package io.knifer.freebox.controller;

import cn.hutool.core.io.FileUtil;
import io.knifer.freebox.component.node.VLCPlayer;
import io.knifer.freebox.constant.ClientType;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.*;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.c2s.FreeBoxLive;
import io.knifer.freebox.model.domain.LiveChannelGroup;
import io.knifer.freebox.service.LoadLiveChannelGroupService;
import io.knifer.freebox.service.VLCPlayerDestroyService;
import io.knifer.freebox.spider.template.SpiderTemplate;
import io.knifer.freebox.util.AsyncUtil;
import io.knifer.freebox.util.CollectionUtil;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.List;

/**
 * 直播
 *
 * @author Knifer
 */
@Slf4j
public class LiveController extends BaseController {

    @FXML
    private VBox root;
    @FXML
    private HBox playerHBox;
    @FXML
    private Label loadingLabel;
    @FXML
    private MenuBar menuBar;
    @FXML
    private Menu switchLiveSourceMenu;

    private final BooleanProperty loadingProperty = new SimpleBooleanProperty(true);

    private VLCPlayer player;

    private Service<List<LiveChannelGroup>> loadLiveChannelGroupService;

    private final static Path LIVE_CONFIG_CACHE_PATH = StorageHelper.getLiveConfigCachePath();

    @FXML
    private void initialize() {
        log.info("LiveController initialize");
        ReadOnlyDoubleProperty rootWidthProp = root.widthProperty();
        ReadOnlyDoubleProperty rootHeightProp = root.heightProperty();
        DoubleBinding playerHBoxHeightProp = rootHeightProp.subtract(menuBar.heightProperty());

        playerHBox.prefWidthProperty().bind(rootWidthProp);
        playerHBox.minWidthProperty().bind(rootWidthProp);
        playerHBox.maxWidthProperty().bind(rootWidthProp);
        bindPlayerHeight(playerHBoxHeightProp);
        loadingLabel.visibleProperty().bind(loadingProperty);
        playerHBox.visibleProperty().bind(loadingProperty.not());
        switchLiveSourceMenu.disableProperty().bind(loadingProperty);
        Platform.runLater(() -> {
            ClientInfo clientInfo = getData();
            ClientType clientType = clientInfo.getClientType();

            WindowHelper.getStage(playerHBox).setOnCloseRequest(event -> destroy());
            if (clientType == ClientType.SINGLE_LIVE) {
                singleLiveClientTypeInitialize(clientInfo, rootHeightProp, playerHBoxHeightProp);
            } else {
                catVodOrTVBoxKClientTypeInitialize(rootHeightProp, playerHBoxHeightProp);
            }
        });
    }

    private void singleLiveClientTypeInitialize(
            ClientInfo clientInfo, ReadOnlyDoubleProperty rootHeightProp, DoubleBinding playerHBoxHeightProp
    ) {
        FreeBoxLive live = FreeBoxLive.from(clientInfo.getConfigUrl());

        switchLiveSourceMenu.setVisible(false);
        setupPlayer(rootHeightProp, playerHBoxHeightProp);
        switchLiveSource(live);
        loadingProperty.set(false);
    }

    private void catVodOrTVBoxKClientTypeInitialize(
            ReadOnlyDoubleProperty rootHeightProp, DoubleBinding playerHBoxHeightProp
    ) {
        SpiderTemplate template = Context.INSTANCE.getSpiderTemplate();

        template.init(callback -> {
            if (!callback) {

                return;
            }
            template.getLives(lives -> {
                if (CollectionUtil.isEmpty(lives)) {
                    Platform.runLater(() -> {
                        ToastHelper.showErrorI18n(I18nKeys.LIVE_MESSAGE_LIVE_NOT_FOUND);
                        loadingProperty.set(false);
                    });

                    return;
                }
                Platform.runLater(() -> {
                    ObservableList<MenuItem> switchLiveSourceMenuItems;
                    ToggleGroup switchLiveSourceToggleGroup = new ToggleGroup();

                    setupPlayer(rootHeightProp, playerHBoxHeightProp);
                    switchLiveSourceMenuItems = switchLiveSourceMenu.getItems();
                    lives.forEach(live -> {
                        RadioMenuItem menuItem = new RadioMenuItem(live.getName());

                        menuItem.setToggleGroup(switchLiveSourceToggleGroup);
                        menuItem.setUserData(live);
                        switchLiveSourceMenuItems.add(menuItem);
                    });
                    switchLiveSourceToggleGroup.selectedToggleProperty()
                            .addListener((ob, oldVal, newVal) ->
                                    switchLiveSource(((FreeBoxLive) newVal.getUserData()))
                            );
                    if (!switchLiveSourceMenuItems.isEmpty()) {
                        switchLiveSourceToggleGroup.selectToggle((Toggle) switchLiveSourceMenuItems.get(0));
                    }
                    loadingProperty.set(false);
                });
            });
        });
    }

    private void setupPlayer(ReadOnlyDoubleProperty rootHeightProp, DoubleBinding playerHBoxHeightProp) {
        player = new VLCPlayer(
                playerHBox,
                VLCPlayer.Config.builder()
                        .liveMode(true)
                        .build()
        );
        player.setOnFullScreen(() -> {
            menuBar.setManaged(false);
            bindPlayerHeight(rootHeightProp);
        });
        player.setOnFullScreenExit(() -> {
            menuBar.setManaged(true);
            bindPlayerHeight(playerHBoxHeightProp);
        });
    }

    private void bindPlayerHeight(ObservableValue<? extends Number> heightProp) {
        playerHBox.prefHeightProperty().unbind();
        playerHBox.prefHeightProperty().bind(heightProp);
        playerHBox.minHeightProperty().unbind();
        playerHBox.minHeightProperty().bind(heightProp);
        playerHBox.maxHeightProperty().unbind();
        playerHBox.maxHeightProperty().bind(heightProp);
    }

    private void destroy() {
        Service<Void> destroyVLCPlayerService;

        if (loadLiveChannelGroupService != null && loadLiveChannelGroupService.isRunning()) {
            loadLiveChannelGroupService.cancel();
        }
        if (player != null) {
            LoadingHelper.showLoading(WindowHelper.getStage(root), I18nKeys.MESSAGE_QUIT_LOADING);
            destroyVLCPlayerService = new VLCPlayerDestroyService(player);
            destroyVLCPlayerService.setOnSucceeded(evt -> {
                AsyncUtil.execute(() -> FileUtil.clean(LIVE_CONFIG_CACHE_PATH.toFile()));
                LoadingHelper.hideLoading();
            });
            destroyVLCPlayerService.start();
        }
        ImageHelper.clearCache();
        Context.INSTANCE.popAndShowLastStage();
    }

    private void switchLiveSource(FreeBoxLive live) {
        loadLiveChannelGroupService = new LoadLiveChannelGroupService(live, LIVE_CONFIG_CACHE_PATH);
        loadingProperty.set(true);
        loadLiveChannelGroupService.setOnSucceeded(evt -> {
            List<LiveChannelGroup> liveChannelGroups = loadLiveChannelGroupService.getValue();
            String epgServiceUrl;

            log.info("switch live source, live: {}, liveChannelGroup count: {}", live, liveChannelGroups.size());
            if (liveChannelGroups.isEmpty()) {
                ToastHelper.showErrorI18n(I18nKeys.LIVE_MESSAGE_LIVE_NOT_FOUND);
                loadingProperty.set(false);

                return;
            }
            player.setLiveChannelGroups(liveChannelGroups);
            epgServiceUrl = live.getEpg();
            player.setEpgServiceUrl(validEpgServiceUrl(epgServiceUrl) ? epgServiceUrl : null);
            player.play(0, 0, 0);
            loadingProperty.set(false);
        });
        loadLiveChannelGroupService.start();
    }

    private boolean validEpgServiceUrl(String epgServiceUrl) {
        return epgServiceUrl != null &&
                epgServiceUrl.startsWith("http") &&
                StringUtils.countMatches(epgServiceUrl, "{name}") == 1 &&
                StringUtils.countMatches(epgServiceUrl, "{date}") == 1;
    }
}
