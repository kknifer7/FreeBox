package io.knifer.freebox.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import io.knifer.freebox.component.node.player.BasePlayer;
import io.knifer.freebox.constant.ClientType;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.ImageHelper;
import io.knifer.freebox.helper.StorageHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.helper.WindowHelper;
import io.knifer.freebox.model.c2s.FreeBoxLive;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.model.domain.ClientLiveProperties;
import io.knifer.freebox.model.domain.LiveChannelGroup;
import io.knifer.freebox.service.LoadLiveChannelGroupService;
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
import org.apache.commons.lang3.tuple.Triple;

import java.nio.file.Path;
import java.util.List;

/**
 * 直播
 *
 * @author Knifer
 */
@Slf4j
public class LiveController implements Destroyable {

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

    private final ToggleGroup switchLiveSourceToggleGroup = new ToggleGroup();

    private final BooleanProperty loadingProperty = new SimpleBooleanProperty(true);

    private BasePlayer<?> player;

    private Service<List<LiveChannelGroup>> loadLiveChannelGroupService;

    private ClientLiveProperties clientLiveProperties;
    private ClientLiveProperties clientLivePropertiesBackup;

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
        Context.INSTANCE.getClientManager().getCurrentClient().thenAccept(clientInfo ->
            Platform.runLater(() -> {
                ClientType clientType;

                if (clientInfo == null) {

                    return;
                }
                clientType = clientInfo.getClientType();
                WindowHelper.getStage(playerHBox).setOnCloseRequest(event -> destroy());
                initProperties(clientInfo);
                if (clientType == ClientType.SINGLE_LIVE) {
                    singleLiveClientTypeInitialize(clientInfo, rootHeightProp, playerHBoxHeightProp);
                } else {
                    catVodOrTVBoxKClientTypeInitialize(rootHeightProp, playerHBoxHeightProp);
                }
            })
        );
    }

    private void singleLiveClientTypeInitialize(
            ClientInfo clientInfo, ReadOnlyDoubleProperty rootHeightProp, DoubleBinding playerHBoxHeightProp
    ) {
        FreeBoxLive live = FreeBoxLive.from(clientInfo.getConfigUrl());

        switchLiveSourceMenu.setVisible(false);
        setupPlayer(rootHeightProp, playerHBoxHeightProp);
        switchLiveSource(live, true);
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
                    String liveSourceName;
                    ObservableList<MenuItem> switchLiveSourceMenuItems;
                    boolean hasLiveSource;

                    setupPlayer(rootHeightProp, playerHBoxHeightProp);
                    switchLiveSourceMenuItems = switchLiveSourceMenu.getItems();
                    lives.forEach(live -> {
                        RadioMenuItem menuItem = new RadioMenuItem(live.getName());

                        menuItem.setToggleGroup(switchLiveSourceToggleGroup);
                        menuItem.setUserData(live);
                        switchLiveSourceMenuItems.add(menuItem);
                    });
                    hasLiveSource = !switchLiveSourceMenuItems.isEmpty();
                    // 尝试恢复上次使用的源
                    liveSourceName = clientLiveProperties.getLiveSourceNameLastUsed();
                    if (StringUtils.isBlank(liveSourceName)) {
                        bindSwitchLiveSourceToggleGroupEventListener(switchLiveSourceToggleGroup);
                        if (hasLiveSource) {
                            switchLiveSourceToggleGroup.selectToggle((Toggle) switchLiveSourceMenuItems.get(0));
                        }
                    } else {
                        if (hasLiveSource) {
                            CollectionUtil.findFirst(
                                    switchLiveSourceMenuItems,
                                    menuItem -> liveSourceName.equals(menuItem.getText())
                            ).ifPresentOrElse(
                                    menuItem -> {
                                        switchLiveSourceToggleGroup.selectToggle((Toggle) menuItem);
                                        switchLiveSource(
                                                ((FreeBoxLive) menuItem.getUserData()), true
                                        );
                                        bindSwitchLiveSourceToggleGroupEventListener(switchLiveSourceToggleGroup);
                                    },
                                    () -> {
                                        bindSwitchLiveSourceToggleGroupEventListener(switchLiveSourceToggleGroup);
                                        switchLiveSource(
                                                ((FreeBoxLive) switchLiveSourceMenuItems.get(0).getUserData()),
                                                false
                                        );
                                    }
                            );
                        } else {
                            bindSwitchLiveSourceToggleGroupEventListener(switchLiveSourceToggleGroup);
                        }
                    }
                    loadingProperty.set(false);
                });
            });
        });
    }

    private void bindSwitchLiveSourceToggleGroupEventListener(ToggleGroup toggleGroup) {
        toggleGroup.selectedToggleProperty()
                .addListener((ob, oldVal, newVal) ->
                        switchLiveSource(
                                ((FreeBoxLive) newVal.getUserData()), false
                        )
                );
    }

    private void setupPlayer(ReadOnlyDoubleProperty rootHeightProp, DoubleBinding playerHBoxHeightProp) {
        player = BasePlayer.createPlayer(
                playerHBox,
                BasePlayer.Config.builder()
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

    private void initProperties(ClientInfo clientInfo) {
        String clientId = clientInfo.getId();

        clientLiveProperties = StorageHelper.find(clientId, ClientLiveProperties.class)
                .orElseGet(() -> ClientLiveProperties.from(clientId));
        clientLivePropertiesBackup = clientLiveProperties.copy();
    }

    @Override
    public void destroy() {
        if (loadLiveChannelGroupService != null && loadLiveChannelGroupService.isRunning()) {
            loadLiveChannelGroupService.cancel();
        }
        if (player != null) {
            saveClientProperties();
            AsyncUtil.execute(() -> {
                FileUtil.clean(LIVE_CONFIG_CACHE_PATH.toFile());
                player.destroy();
            });
        }
        ImageHelper.clearCache();
        Context.INSTANCE.popAndShowLastStage();
    }

    private void saveClientProperties() {
        Toggle toggle;
        String liveSourceName;
        Triple<String, String, String> triple;
        ClientInfo clientInfo = Context.INSTANCE.getClientManager().getCurrentClientImmediately();

        if (clientInfo == null) {

            return;
        }
        if (clientInfo.getClientType() == ClientType.SINGLE_LIVE) {
            liveSourceName = null;
        } else {
            toggle = switchLiveSourceToggleGroup.getSelectedToggle();
            if (toggle == null) {

                return;
            }
            liveSourceName = ((FreeBoxLive) toggle.getUserData()).getName();
            if (StringUtils.isBlank(liveSourceName)) {

                return;
            }
        }
        triple = player.getCurrentLiveInfo();
        if (triple == null) {
            StorageHelper.delete(clientLiveProperties);

            return;
        }
        clientLiveProperties.setLiveSourceNameLastUsed(liveSourceName);
        clientLiveProperties.setLiveChannelGroupTitleLastUsed(triple.getLeft());
        clientLiveProperties.setLiveChannelTitleLastUsed(triple.getMiddle());
        clientLiveProperties.setLiveChannelLineTitleLastUsed(triple.getRight());
        if (!clientLiveProperties.equals(clientLivePropertiesBackup)) {
            StorageHelper.save(clientLiveProperties);
        }
    }

    /**
     * 切换直播源
     * @param live 直播源
     * @param tryApplyClientPropertiesFlag 是否尝试应用客户端属性（仅在初始化阶段可能为true）
     */
    private void switchLiveSource(FreeBoxLive live, boolean tryApplyClientPropertiesFlag) {
        loadLiveChannelGroupService = new LoadLiveChannelGroupService(live, LIVE_CONFIG_CACHE_PATH);
        loadingProperty.set(true);
        loadLiveChannelGroupService.setOnSucceeded(evt -> {
            List<LiveChannelGroup> liveChannelGroups = loadLiveChannelGroupService.getValue();
            String epgServiceUrl;
            Triple<Integer, Integer, Integer> channelGroupIdxAndChannelIdxAndLineIdx;

            log.info(
                    "switch live source, tryApplyClientProperties: {}, live: {}, liveChannelGroup count: {}",
                    tryApplyClientPropertiesFlag, live, liveChannelGroups.size()
            );
            if (liveChannelGroups.isEmpty()) {
                ToastHelper.showErrorI18n(I18nKeys.LIVE_MESSAGE_LIVE_NOT_FOUND);
                loadingProperty.set(false);

                return;
            }
            player.setLiveChannelGroups(liveChannelGroups);
            epgServiceUrl = live.getEpg();
            player.setEpgServiceUrl(validEpgServiceUrl(epgServiceUrl) ? epgServiceUrl : null);
            if (tryApplyClientPropertiesFlag) {
                channelGroupIdxAndChannelIdxAndLineIdx =
                        getChannelGroupIdxAndChannelIdxAndLineIdxByClientProperties(liveChannelGroups);
                player.play(
                        channelGroupIdxAndChannelIdxAndLineIdx.getLeft(),
                        channelGroupIdxAndChannelIdxAndLineIdx.getMiddle(),
                        channelGroupIdxAndChannelIdxAndLineIdx.getRight()
                );
            } else {
                player.play(0, 0, 0);
            }
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

    /**
     * 根据客户端属性，从频道组数据中获取上次播放的频道组、频道、线路的索引
     * @param liveChannelGroups 频道组
     * @return 频道组、频道、线路的索引
     */
    private Triple<Integer, Integer, Integer> getChannelGroupIdxAndChannelIdxAndLineIdxByClientProperties(
            List<LiveChannelGroup> liveChannelGroups
    ) {
        String lastChannelGroupTitle;
        int channelGroupIdx;
        String lastChannelTitle;
        int channelIdx;
        String lastLineTitle;
        int lineIdx;

        lastChannelGroupTitle = clientLiveProperties.getLiveChannelGroupTitleLastUsed();
        if (StringUtils.isBlank(lastChannelGroupTitle)) {

            return Triple.of(0, 0, 0);
        }
        channelGroupIdx = CollUtil.indexOf(
                liveChannelGroups,
                liveChannelGroup -> lastChannelGroupTitle.equals(liveChannelGroup.getTitle())
        );
        if (channelGroupIdx == -1) {

            return Triple.of(0, 0, 0);
        }
        lastChannelTitle = clientLiveProperties.getLiveChannelTitleLastUsed();
        if (StringUtils.isBlank(lastChannelTitle)) {

            return Triple.of(channelGroupIdx, 0, 0);
        }
        channelIdx = CollUtil.indexOf(
                liveChannelGroups.get(channelGroupIdx).getChannels(),
                liveChannel -> lastChannelTitle.equals(liveChannel.getTitle())
        );
        if (channelIdx == -1) {

            return Triple.of(channelGroupIdx, 0, 0);
        }
        lastLineTitle = clientLiveProperties.getLiveChannelLineTitleLastUsed();
        if (StringUtils.isBlank(lastLineTitle)) {

            return Triple.of(channelGroupIdx, channelIdx, 0);
        }
        lineIdx = CollUtil.indexOf(
                liveChannelGroups.get(channelGroupIdx).getChannels().get(channelIdx).getLines(),
                liveChannelLine -> lastLineTitle.equals(liveChannelLine.getTitle())
        );

        return Triple.of(channelGroupIdx, channelIdx, Math.max(lineIdx, 0));
    }
}
