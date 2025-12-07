package io.knifer.freebox.component.node.player;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import io.knifer.freebox.component.node.LogoPane;
import io.knifer.freebox.constant.BaseResources;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.Views;
import io.knifer.freebox.controller.EPGOverviewController;
import io.knifer.freebox.handler.EpgFetchingHandler;
import io.knifer.freebox.handler.impl.ParameterizedEpgFetchingHandler;
import io.knifer.freebox.helper.*;
import io.knifer.freebox.model.bo.EPGOverviewBO;
import io.knifer.freebox.model.bo.TVPlayBO;
import io.knifer.freebox.model.common.diyp.EPG;
import io.knifer.freebox.model.domain.LiveChannel;
import io.knifer.freebox.model.domain.LiveChannelGroup;
import io.knifer.freebox.util.AsyncUtil;
import io.knifer.freebox.util.CastUtil;
import io.knifer.freebox.util.CollectionUtil;
import io.knifer.freebox.util.FXMLUtil;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.*;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Callback;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.ToggleSwitch;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 播放器基类
 * 若无注释说明，此基类中的方法均不允许阻塞
 * PS：我知道这里的代码很乱，没有注释，因为我没指望谁有心思动这个类。如果你有什么想法，直接告诉我。
 * @author Knifer
 */
@Slf4j
public abstract class BasePlayer<T extends Node> {

    protected final T playerNode;
    protected final StackPane playerPane;
    protected final Pane parent;
    protected final Config config;

    private final Stage stage;
    private final Scene scene;
    private final AnchorPane toastAnchorPane;
    private final PlayerToastPane toastPane;
    private final AnchorPane controlPane;
    private final Timer controlPaneHideTimer = new Timer(2000, evt -> setControlsVisible(false));
    private final Timer volumePopOverHideTimer;
    private final Timer settingsPopOverHideTimer;
    private final ProgressIndicator loadingProgressIndicator;
    private final Label loadingProgressLabel;
    private final ImageView pausedPlayButtonImageView;
    private final Label loadingErrorIconLabel;
    private final Label loadingErrorLabel;
    private final VBox loadingErrorVBox;
    private final Label pauseLabel;
    private final Label stepBackwardLabel;
    private final Label stepForwardLabel;
    private final Slider volumeSlider;
    private final Label volumeLabel;
    private final ToggleGroup rateSettingToggleGroup;
    private final RadioButton rate0_5SettingRadioButton;
    private final RadioButton rate1SettingRadioButton;
    private final RadioButton rate1_25SettingRadioButton;
    private final RadioButton rate1_5SettingRadioButton;
    private final RadioButton rate2SettingRadioButton;
    private final ToggleSwitch fillWindowToggleSwitch;
    private final Button reloadButton;
    private final Button subtitleButton;
    private final Button danMaKuButton;
    private final PlayerSubtitleSettingPopOver subtitleSettingPopOver;
    private final Label settingsLabel;
    private final PlayerLiveChannelLinesWithPaginator liveChannelLinesWithPaginator;
    private final ProgressBar videoProgressBar;
    private final Label videoProgressLabel;
    private final Label videoProgressSplitLabel;
    private final Label videoProgressLengthLabel;
    private final Label fullScreenLabel;
    private final Label videoTitleLabel;
    private final Label epgOpenLabel;
    private final HBox controlTopHBox;
    private final LiveChannelBanner liveChannelBanner;
    private final LiveDrawer liveChannelDrawer;
    private final EpgFetchingHandler epgFetchingHandler;
    private final DateTimeFormatter LOCAL_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private final FontIcon pauseIcon = FontIcon.of(FontAwesome.PAUSE, 32, Color.WHITE);
    private final FontIcon playIcon = FontIcon.of(FontAwesome.PLAY, 32, Color.WHITE);
    private final FontIcon stepBackwardIcon = FontIcon.of(FontAwesome.STEP_BACKWARD, 32, Color.WHITE);
    private final FontIcon stepForwardIcon = FontIcon.of(FontAwesome.STEP_FORWARD, 32, Color.WHITE);
    private final FontIcon volumeOnIcon = FontIcon.of(FontAwesome.VOLUME_UP, 32, Color.WHITE);
    private final FontIcon volumeOffIcon = FontIcon.of(FontAwesome.VOLUME_OFF, 32, Color.WHITE);
    private final FontIcon fullScreenIcon = FontIcon.of(FontAwesome.ARROWS_ALT, 32, Color.WHITE);
    private final FontIcon reloadIcon = FontIcon.of(FontAwesome.REFRESH, 16);
    private final FontIcon settingsIcon = FontIcon.of(FontAwesome.SLIDERS, 32, Color.WHITE);
    private final AtomicLong videoLength = new AtomicLong(-1);
    private final AtomicLong initProgress = new AtomicLong(-1);
    private final AtomicBoolean isVideoProgressBarUsing = new AtomicBoolean(false);
    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);
    private final BooleanProperty isError = new SimpleBooleanProperty(false);
    private final SimpleStringProperty epgServiceUrlProperty = new SimpleStringProperty();

    private final DoubleBinding paneWidthProp;

    protected volatile boolean destroyFlag = false;

    private List<LiveChannelGroup> liveChannelGroups = null;
    private LiveInfoBO selectedLive = null;
    private LiveInfoBO playingLive = null;

    private Stage epgStage;

    private Runnable stepBackwardRunnable = BaseValues.EMPTY_RUNNABLE;
    private Runnable stepForwardRunnable = BaseValues.EMPTY_RUNNABLE;
    private Runnable fullScreenRunnable = BaseValues.EMPTY_RUNNABLE;
    private Runnable fullScreenExitRunnable = BaseValues.EMPTY_RUNNABLE;

    public BasePlayer(Pane parent, Config config) {
        boolean liveMode = BooleanUtils.toBoolean(config.getLiveMode());
        boolean externalMode = BooleanUtils.toBoolean(config.getExternalMode());
        ObservableList<Node> parentChildren = parent.getChildren();
        ReadOnlyDoubleProperty parentWidthProp = parent.widthProperty();
        ReadOnlyDoubleProperty parentHeightProp = parent.heightProperty();
        URL stylesheetUrl;
        List<Node> paneChildren;
        VBox volumePopOverInnerVBox;
        PopOver volumePopOver;
        Label rateSettingTitleLabel;
        HBox rateSettingRadioButtonHBox;
        HBox rateSettingHBox;
        Label reloadSettingTitleLabel;
        HBox reloadSettingsHBox;
        Label subtitleAndDanMaKuLabel;
        ToggleSwitch subtitleAndDanMaKuToggleSwitch = new ToggleSwitch();
        HBox subtitleAndDanMaKuControlHBox;
        HBox subtitleAndDanMaKuHBox;
        VBox settingsPopOverInnerVBox;
        PopOver settingsPopOver;
        HBox progressLabelHBox;
        HBox leftToolBarHbox;
        HBox rightToolBarHbox;
        AnchorPane controlBottomAnchorPane;
        StackPane progressMiddleStackPane;
        AnchorPane controlTopAnchorPane;

        this.parent = parent;
        this.config = config;
        stage = WindowHelper.getStage(parent);
        scene = stage.getScene();
        stylesheetUrl = BaseResources.PLAYER_CSS;
        scene.getStylesheets().add(stylesheetUrl.toExternalForm());
        playerPane = new StackPane();
        stage.setFullScreenExitHint(StringUtils.EMPTY);
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        toastPane = new PlayerToastPane();
        toastAnchorPane = new AnchorPane(toastPane);
        AnchorPane.setBottomAnchor(toastPane, 50.0);
        AnchorPane.setLeftAnchor(toastPane, 10.0);
        loadingProgressIndicator = new ProgressIndicator();
        loadingProgressIndicator.visibleProperty().bind(isLoading.and(isError.not()));
        loadingProgressLabel = new Label();
        loadingProgressLabel.setVisible(false);
        loadingProgressLabel.getStyleClass().add("dodge-blue");
        pausedPlayButtonImageView = new ImageView(BaseResources.PLAY_BUTTON_IMG);
        pausedPlayButtonImageView.setFitWidth(64);
        pausedPlayButtonImageView.setFitHeight(64);
        pausedPlayButtonImageView.setPreserveRatio(true);
        pausedPlayButtonImageView.setVisible(false);
        loadingErrorIconLabel = new Label();
        loadingErrorIconLabel.setGraphic(FontIcon.of(FontAwesome.WARNING, 32, Color.WHITE));
        loadingErrorLabel = new Label(I18nHelper.get(I18nKeys.COMMON_VIDEO_LOADING_ERROR));
        loadingErrorLabel.getStyleClass().add("player-loading-error-label");
        loadingErrorVBox = new VBox(loadingErrorIconLabel, loadingErrorLabel);
        loadingErrorVBox.setSpacing(3);
        loadingErrorVBox.setAlignment(Pos.CENTER);
        loadingErrorVBox.visibleProperty().bind(isLoading.not().and(isError));
        // 暂停设置
        pauseLabel = new Label();
        pauseLabel.setGraphic(pauseIcon);
        pauseLabel.getStyleClass().add("player-control-label");
        pauseLabel.setOnMouseClicked(evt -> {
            if (evt.getButton() == MouseButton.PRIMARY) {
                togglePause();
            }
        });
        // 上一集、下一集
        stepBackwardLabel = new Label();
        stepBackwardLabel.setGraphic(stepBackwardIcon);
        stepBackwardLabel.getStyleClass().add("player-control-label");
        stepForwardLabel = new Label();
        stepForwardLabel.setGraphic(stepForwardIcon);
        stepForwardLabel.getStyleClass().add("player-control-label");
        if (liveMode) {
            epgFetchingHandler = ParameterizedEpgFetchingHandler.getInstance();
            selectedLive = new LiveInfoBO();
            playingLive = new LiveInfoBO();
            stepBackwardLabel.setOnMouseClicked(evt -> {
                LiveChannelGroup channelGroup;
                int groupIdx;
                List<LiveChannel> liveChannels;
                LiveChannel liveChannel;
                int currentLiveChannelIdx;
                int channelIdx;

                if (CollectionUtil.isNotEmpty(liveChannelGroups)) {
                    channelGroup = playingLive.getLiveChannelGroup();
                    if (channelGroup != null) {
                        groupIdx = liveChannelGroups.indexOf(channelGroup);
                        liveChannels = channelGroup.getChannels();
                        liveChannel = playingLive.getLiveChannel();
                        currentLiveChannelIdx = liveChannels.indexOf(liveChannel);
                        if (currentLiveChannelIdx == 0) {
                            groupIdx--;
                            channelGroup = groupIdx > -1 ? CollUtil.get(liveChannelGroups, groupIdx) : null;
                        }
                        if (channelGroup != null) {
                            liveChannels = channelGroup.getChannels();
                            liveChannel = CollUtil.get(liveChannels, currentLiveChannelIdx);
                            channelIdx = currentLiveChannelIdx == 0 ?
                                    liveChannels.size() - 1 : currentLiveChannelIdx - 1;
                            if (liveChannel != null && CollUtil.get(liveChannels, channelIdx) != null) {
                                play(groupIdx, channelIdx, 0);
                            }
                        }
                    }
                    stepBackwardRunnable.run();
                }
            });
            stepForwardLabel.setOnMouseClicked(evt -> {
                LiveChannelGroup channelGroup;
                int groupIdx;
                List<LiveChannel> liveChannels;
                LiveChannel liveChannel;
                int currentLiveChannelIdx;
                int channelIdx;

                if (CollectionUtil.isNotEmpty(liveChannelGroups)) {
                    channelGroup = playingLive.getLiveChannelGroup();
                    if (channelGroup != null) {
                        groupIdx = liveChannelGroups.indexOf(channelGroup);
                        liveChannels = channelGroup.getChannels();
                        liveChannel = playingLive.getLiveChannel();
                        currentLiveChannelIdx = liveChannels.indexOf(liveChannel);
                        if (currentLiveChannelIdx == liveChannels.size() - 1) {
                            groupIdx++;
                            channelGroup = CollUtil.get(liveChannelGroups, groupIdx);
                            currentLiveChannelIdx = -1;
                        }
                        if (channelGroup != null) {
                            liveChannels = channelGroup.getChannels();
                            liveChannel = CollUtil.get(liveChannels, currentLiveChannelIdx);
                            channelIdx = currentLiveChannelIdx + 1;
                            if (liveChannel != null && CollUtil.get(liveChannels, channelIdx) != null) {
                                play(groupIdx, channelIdx, 0);
                            }
                        }
                    }
                }
                stepForwardRunnable.run();
            });
            subtitleButton = null;
            danMaKuButton = null;
            subtitleSettingPopOver = null;
            subtitleAndDanMaKuHBox = null;
        } else {
            epgFetchingHandler = null;
            stepBackwardLabel.setOnMouseClicked(evt -> {
                if (evt.getButton() == MouseButton.PRIMARY) {
                    stepBackwardRunnable.run();
                }
            });
            stepForwardLabel.setOnMouseClicked(evt -> {
                if (evt.getButton() == MouseButton.PRIMARY) {
                    stepForwardRunnable.run();
                }
            });
            subtitleButton = new Button();
            danMaKuButton = new Button();
            subtitleSettingPopOver = new PlayerSubtitleSettingPopOver();
            subtitleSettingPopOver.setOnSubtitleDelayChanged(delay -> {
                log.info("subtitle delay: {}", delay);
                toastPane.showToast(String.format(
                        I18nHelper.get(I18nKeys.VIDEO_SETTINGS_SUBTITLE_MESSAGE_UPDATE_DELAY_VALUE),
                        delay
                ), 2);
                setSubtitleDelay(delay);
            });
            subtitleSettingPopOver.setOnSubtitleFileChosen(subtitleFile -> {
                if (useSubtitle(subtitleFile)) {
                    log.info("using subtitle file: {}", subtitleFile);
                    toastPane.showToast(String.format(
                            I18nHelper.get(I18nKeys.VIDEO_SETTINGS_SUBTITLE_MESSAGE_SUCCEED),
                            subtitleFile.getName()
                    ));
                    subtitleAndDanMaKuToggleSwitch.setDisable(false);
                }
            });
            subtitleAndDanMaKuLabel = new Label(I18nHelper.get(I18nKeys.VIDEO_SETTINGS_EXTERNAL));
            subtitleButton.setText(I18nHelper.get(I18nKeys.VIDEO_SETTINGS_SUBTITLE));
            subtitleButton.setFocusTraversable(false);
            subtitleButton.setOnAction(evt -> subtitleSettingPopOver.show(stage));
            danMaKuButton.setText(I18nHelper.get(I18nKeys.VIDEO_SETTINGS_DANMAKU));
            danMaKuButton.setFocusTraversable(false);
            danMaKuButton.setOnAction(evt -> subtitleSettingPopOver.show(stage));
            subtitleAndDanMaKuToggleSwitch.setDisable(true);
            subtitleAndDanMaKuToggleSwitch.setSelected(true);
            subtitleAndDanMaKuToggleSwitch.selectedProperty()
                    .addListener((ob, oldVal, newVal) -> {
                        setSubtitleVisible(newVal);
                        toastPane.showToast(
                                I18nHelper.get(I18nKeys.VIDEO_SETTINGS_SUBTITLE) +
                                        " " +
                                        (newVal ?
                                                I18nHelper.get(I18nKeys.COMMON_ON) :
                                                I18nHelper.get(I18nKeys.COMMON_OFF)
                                        )
                        );
                    });
            subtitleAndDanMaKuControlHBox = new HBox(subtitleButton, danMaKuButton, subtitleAndDanMaKuToggleSwitch);
            subtitleAndDanMaKuControlHBox.setSpacing(5);
            subtitleAndDanMaKuControlHBox.setAlignment(Pos.CENTER_LEFT);
            subtitleAndDanMaKuHBox = new HBox(subtitleAndDanMaKuLabel, subtitleAndDanMaKuControlHBox);
            subtitleAndDanMaKuHBox.setSpacing(15);
            subtitleAndDanMaKuHBox.setAlignment(Pos.CENTER_LEFT);
        }
        // 音量设置
        volumeLabel = new Label();
        volumeLabel.setGraphic(volumeOnIcon);
        volumeLabel.getStyleClass().add("player-control-label");
        volumeLabel.setOnMouseClicked(evt -> {
            volumeLabel.setGraphic(isMute() ? volumeOnIcon : volumeOffIcon);
            toggleMute();
        });
        volumeSlider = new Slider(0, 100, 100);
        volumeSlider.setOrientation(Orientation.VERTICAL);
        volumePopOverInnerVBox = new VBox(volumeSlider);
        volumePopOverInnerVBox.setAlignment(Pos.CENTER);
        volumePopOver = new PopOver(volumePopOverInnerVBox);
        volumePopOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
        volumePopOver.getStyleClass().add("player-pop-over");
        volumePopOver.setDetachable(false);
        volumePopOverHideTimer = new Timer(1000, evt -> volumePopOver.hide());
        volumePopOverInnerVBox.addEventFilter(MouseEvent.ANY, evt -> {
            EventType<? extends MouseEvent> eventType = evt.getEventType();

            if (eventType == MouseEvent.MOUSE_ENTERED) {
                setControlsAutoHide(false);
                volumePopOverHideTimer.stop();
                if (!volumePopOver.isShowing()) {
                    volumePopOver.show(volumeLabel);
                }
            } else if (eventType == MouseEvent.MOUSE_EXITED) {
                setControlsAutoHide(true);
                volumePopOverHideTimer.start();
            }
        });
        volumeSlider.valueProperty().addListener((ob, oldVal, newVal) -> {
            int volume = newVal.intValue();

            setVolume(volume);
            if (isMute()) {
                // 如果是静音状态，解除静音状态
                toggleMute();
                volumeLabel.setGraphic(volumeOnIcon);
            }
        });
        volumeLabel.setOnMouseEntered(evt -> {
            volumePopOverHideTimer.restart();
            if (!volumePopOver.isShowing()) {
                volumePopOver.show(volumeLabel);
            }
        });
        settingsLabel = new Label();
        settingsLabel.getStyleClass().add("player-control-label");
        settingsLabel.setGraphic(settingsIcon);
        // 倍速设置
        rateSettingTitleLabel = new Label(I18nHelper.get(I18nKeys.VIDEO_SETTINGS_RATE));
        HBox.setMargin(rateSettingTitleLabel, new Insets(0, 10, 0, 0));
        rateSettingToggleGroup = new ToggleGroup();
        rate0_5SettingRadioButton = new RadioButton("0.5");
        rate0_5SettingRadioButton.setUserData(0.5f);
        rate0_5SettingRadioButton.setToggleGroup(rateSettingToggleGroup);
        rate1SettingRadioButton = new RadioButton("1.0");
        rate1SettingRadioButton.setUserData(1.0f);
        rate1SettingRadioButton.setToggleGroup(rateSettingToggleGroup);
        rate1_25SettingRadioButton = new RadioButton("1.25");
        rate1_25SettingRadioButton.setUserData(1.25f);
        rate1_25SettingRadioButton.setToggleGroup(rateSettingToggleGroup);
        rate1_5SettingRadioButton = new RadioButton("1.5");
        rate1_5SettingRadioButton.setUserData(1.5f);
        rate1_5SettingRadioButton.setToggleGroup(rateSettingToggleGroup);
        rate2SettingRadioButton = new RadioButton("2.0");
        rate2SettingRadioButton.setUserData(2.0f);
        rate2SettingRadioButton.setToggleGroup(rateSettingToggleGroup);
        // 默认选择1倍速
        rateSettingToggleGroup.selectToggle(rate1SettingRadioButton);
        rateSettingToggleGroup.selectedToggleProperty().addListener(((observable, oldValue, newValue) ->
                setRate((float) newValue.getUserData())
        ));
        rateSettingRadioButtonHBox = new HBox(
                rate0_5SettingRadioButton,
                rate1SettingRadioButton,
                rate1_25SettingRadioButton,
                rate1_5SettingRadioButton,
                rate2SettingRadioButton
        );
        rateSettingHBox = new HBox(rateSettingTitleLabel, rateSettingRadioButtonHBox);
        rateSettingHBox.setSpacing(10);
        rateSettingHBox.setAlignment(Pos.CENTER);
        rateSettingRadioButtonHBox.setAlignment(Pos.CENTER);
        rateSettingRadioButtonHBox.setSpacing(5);
        // 铺满设置按钮
        fillWindowToggleSwitch = new ToggleSwitch(I18nHelper.get(I18nKeys.VIDEO_SETTINGS_FILL_WINDOW));
        fillWindowToggleSwitch.setFocusTraversable(false);
        fillWindowToggleSwitch.selectedProperty()
                .addListener((ob, oldVal, newVal) -> setFillWindow(newVal));
        // 重新加载
        reloadSettingTitleLabel = new Label(I18nHelper.get(I18nKeys.VIDEO_SETTINGS_RELOAD));
        reloadButton = new Button();
        reloadButton.setGraphic(reloadIcon);
        reloadButton.setFocusTraversable(false);
        reloadButton.setOnAction(evt -> {
            initProgress.set(getCurrentProgress());
            stop();
            play();
        });
        reloadSettingsHBox = new HBox(reloadSettingTitleLabel, reloadButton);
        reloadSettingsHBox.setSpacing(15);
        reloadSettingsHBox.setAlignment(Pos.CENTER_LEFT);
        // 设置弹出框
        settingsPopOver = new PopOver();
        settingsPopOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
        settingsPopOver.getStyleClass().add("player-pop-over");
        settingsPopOver.setDetachable(false);
        settingsPopOverHideTimer = new Timer(1000, evt -> settingsPopOver.hide());
        settingsPopOverInnerVBox = liveMode ?
                new VBox(reloadSettingsHBox, fillWindowToggleSwitch, rateSettingHBox) :
                new VBox(subtitleAndDanMaKuHBox, reloadSettingsHBox, fillWindowToggleSwitch, rateSettingHBox);
        settingsPopOverInnerVBox.setSpacing(10.0);
        settingsPopOverInnerVBox.addEventFilter(MouseEvent.ANY, evt -> {
            EventType<? extends MouseEvent> eventType = evt.getEventType();

            if (eventType == MouseEvent.MOUSE_ENTERED) {
                setControlsAutoHide(false);
                settingsPopOverHideTimer.stop();
                if (!settingsPopOver.isShowing()) {
                    settingsPopOver.show(settingsLabel);
                }
            } else if (eventType == MouseEvent.MOUSE_EXITED) {
                setControlsAutoHide(true);
                settingsPopOverHideTimer.start();
            }
        });
        settingsPopOver.setContentNode(settingsPopOverInnerVBox);
        settingsLabel.setOnMouseEntered(evt -> {
            settingsPopOverHideTimer.restart();
            if (!settingsPopOver.isShowing()) {
                settingsPopOver.show(settingsLabel);
            }
        });
        // 铺满、全屏组件
        fullScreenLabel = new Label();
        fullScreenLabel.getStyleClass().add("player-control-label");
        fullScreenLabel.setGraphic(fullScreenIcon);
        fullScreenLabel.setOnMouseClicked(evt -> toggleFullScreen());
        rightToolBarHbox = new HBox(fullScreenLabel);
        rightToolBarHbox.setSpacing(20);
        if (liveMode) {
            // 直播模式，不用显示进度条相关组件
            videoProgressBar = null;
            videoProgressLabel = null;
            videoProgressSplitLabel = null;
            videoProgressLengthLabel = null;
            progressLabelHBox = null;
            liveChannelLinesWithPaginator = new PlayerLiveChannelLinesWithPaginator(
                    line -> play(
                            playingLive.getLiveChannelGroup(),
                            playingLive.getLiveChannel(),
                            line
                    )
            );
            leftToolBarHbox = new HBox(
                    pauseLabel, stepBackwardLabel, stepForwardLabel, volumeLabel, settingsLabel, liveChannelLinesWithPaginator
            );
            controlBottomAnchorPane = new AnchorPane(leftToolBarHbox, rightToolBarHbox);
        } else {
            liveChannelLinesWithPaginator = null;
            // 进度条组件
            videoProgressBar = new ProgressBar(0);
            videoProgressLabel = new Label("00:00:00");
            videoProgressLabel.getStyleClass().add("player-progress-label");
            videoProgressBar.setOnMousePressed(evt -> {
                double newProgress;

                if (!isSeekable()) {
                    return;
                }
                isVideoProgressBarUsing.set(true);
                // 让播放器的控制面板保持可见
                setControlsAutoHide(false);
                // 处理进度拖动相关逻辑
                newProgress = evt.getX() / videoProgressBar.getWidth();
                videoProgressBar.setProgress(newProgress);
                if (newProgress > 0) {
                    videoProgressLabel.setText(formatProgressText((long) (videoLength.get() * newProgress)));
                }
            });
            videoProgressBar.setOnMouseDragged(evt -> {
                double newX;
                double width;
                double newProgress;

                if (!isSeekable()) {
                    return;
                }
                newX = evt.getX();
                width = videoProgressBar.getWidth();
                newProgress = newX / width;
                if (newProgress > 0) {
                    if (newProgress > 1) {
                        videoProgressLabel.setText(formatProgressText(videoLength.get()));
                        videoProgressBar.setProgress(1);
                    } else {
                        videoProgressLabel.setText(formatProgressText((long) (videoLength.get() * newProgress)));
                        videoProgressBar.setProgress(newProgress);
                    }
                } else {
                    videoProgressLabel.setText("00:00:00");
                    videoProgressBar.setProgress(0);
                }
            });
            videoProgressBar.setOnMouseReleased(evt -> {
                if (!isSeekable()) {
                    return;
                }
                setLoading(true);
                // 恢复播放器的控制面板自动隐藏逻辑
                setControlsAutoHide(true);
                // 处理进度拖动相关逻辑
                setPositionPercent((float) videoProgressBar.getProgress());
                isVideoProgressBarUsing.set(false);
            });
            videoProgressBar.disableProperty().bind(isLoading.and(isError.not()));
            videoProgressSplitLabel = new Label("/");
            videoProgressSplitLabel.getStyleClass().add("player-progress-label");
            videoProgressLengthLabel = new Label("-:-:-");
            videoProgressLengthLabel.getStyleClass().add("player-progress-label");
            progressLabelHBox = new HBox(videoProgressLabel, videoProgressSplitLabel, videoProgressLengthLabel);
            progressLabelHBox.setSpacing(5);
            progressLabelHBox.setAlignment(Pos.CENTER);
            leftToolBarHbox = new HBox(pauseLabel, stepForwardLabel, volumeLabel, settingsLabel, progressLabelHBox);
            controlBottomAnchorPane = new AnchorPane(leftToolBarHbox, videoProgressBar, rightToolBarHbox);
            AnchorPane.setLeftAnchor(videoProgressBar, 490.0);
            AnchorPane.setRightAnchor(videoProgressBar, 70.0);
            AnchorPane.setTopAnchor(videoProgressBar, 10.0);
            AnchorPane.setBottomAnchor(videoProgressBar, 10.0);
        }
        leftToolBarHbox.setSpacing(20);
        leftToolBarHbox.setAlignment(Pos.CENTER);
        controlBottomAnchorPane.getStyleClass().add("player-anchor-pane");
        controlBottomAnchorPane.setOnMouseClicked(Event::consume);
        AnchorPane.setLeftAnchor(leftToolBarHbox, 10.0);
        AnchorPane.setTopAnchor(leftToolBarHbox, 10.0);
        AnchorPane.setBottomAnchor(leftToolBarHbox, 10.0);
        AnchorPane.setRightAnchor(rightToolBarHbox, 10.0);
        AnchorPane.setTopAnchor(rightToolBarHbox, 10.0);
        AnchorPane.setBottomAnchor(rightToolBarHbox, 10.0);
        // 顶端标题
        videoTitleLabel = new Label();
        videoTitleLabel.getStyleClass().add("player-title");
        controlTopHBox = new HBox(videoTitleLabel);
        controlTopHBox.setSpacing(10);
        controlTopAnchorPane = new AnchorPane(controlTopHBox);
        controlTopAnchorPane.getStyleClass().add("player-anchor-pane");
        controlTopAnchorPane.setOnMouseClicked(Event::consume);
        AnchorPane.setLeftAnchor(controlTopHBox, 10.0);
        AnchorPane.setRightAnchor(controlTopHBox, 10.0);
        AnchorPane.setTopAnchor(controlTopHBox, 10.0);
        AnchorPane.setBottomAnchor(controlTopHBox, 10.0);
        controlPane = new AnchorPane(controlBottomAnchorPane, controlTopAnchorPane);
        AnchorPane.setLeftAnchor(controlBottomAnchorPane, 0.0);
        AnchorPane.setRightAnchor(controlBottomAnchorPane, 0.0);
        AnchorPane.setBottomAnchor(controlBottomAnchorPane, 0.0);
        AnchorPane.setLeftAnchor(controlTopAnchorPane, 0.0);
        AnchorPane.setRightAnchor(controlTopAnchorPane, 0.0);
        AnchorPane.setTopAnchor(controlTopAnchorPane, 0.0);
        progressMiddleStackPane = new StackPane(
                loadingProgressIndicator,
                loadingProgressLabel,
                pausedPlayButtonImageView,
                loadingErrorVBox
        );
        paneChildren = playerPane.getChildren();
        playerNode = createPlayerNode();
        paneChildren.add(playerNode);
        paneChildren.add(progressMiddleStackPane);
        paneChildren.add(toastAnchorPane);
        paneChildren.add(controlPane);
        if (liveMode) {
            epgOpenLabel = new Label(I18nHelper.get(I18nKeys.LIVE_PLAYER_EPG));
            epgOpenLabel.getStyleClass().add("player-live-epg-label");
            epgOpenLabel.setOnMouseClicked(evt -> {
                Pair<Stage, EPGOverviewController> stageAndController = FXMLUtil.load(Views.EPG_OVERVIEW);

                stageAndController.getRight()
                        .setData(EPGOverviewBO.of(
                                liveChannelGroups, playingLive.getLiveChannel(), epgServiceUrlProperty.get()
                        ));
                epgStage = stageAndController.getLeft();
                epgStage.show();
            });
            epgOpenLabel.disableProperty().bind(epgServiceUrlProperty.isEmpty());
            controlTopHBox.getChildren().add(epgOpenLabel);
            liveChannelBanner = new LiveChannelBanner();
            StackPane.setMargin(liveChannelBanner, new Insets(0, 0, 50, 0));
            StackPane.setAlignment(liveChannelBanner, Pos.BOTTOM_CENTER);
            paneChildren.add(liveChannelBanner);
            liveChannelDrawer = new LiveDrawer(selectedLive, playingLive, playerPane, this);
            liveChannelDrawer.addEventFilter(MouseEvent.ANY, evt -> {
                EventType<? extends MouseEvent> eventType = evt.getEventType();

                if (eventType == MouseEvent.MOUSE_ENTERED) {
                    setControlsAutoHide(false);
                } else if (eventType == MouseEvent.MOUSE_EXITED) {
                    setControlsAutoHide(true);
                } else if (eventType == MouseEvent.MOUSE_MOVED) {
                    // 消费掉鼠标移动事件，避免controls被唤起
                    evt.consume();
                }
            });
            paneChildren.add(liveChannelDrawer);
            StackPane.setAlignment(liveChannelDrawer, Pos.CENTER_LEFT);
        } else {
            epgOpenLabel = null;
            liveChannelBanner = null;
            liveChannelDrawer = null;
        }
        playerPane.getStyleClass().add("player");
        paneWidthProp = liveMode ? parentWidthProp.multiply(1) : parentWidthProp.multiply(0.8);
        bindPlayerPaneWidth(paneWidthProp);
        playerPane.prefHeightProperty().bind(parentHeightProp);
        playerPane.minHeightProperty().bind(parentHeightProp);
        playerPane.maxHeightProperty().bind(parentHeightProp);
        playerPane.setOnMouseClicked(evt -> {
            if (evt.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (evt.getClickCount() == 1) {
                togglePause();
            } else {
                togglePause();
                toggleFullScreen();
            }
        });
        if (externalMode) {
            // 使用外部播放器，隐藏用不上的控件
            progressMiddleStackPane.setVisible(false);
            settingsLabel.setVisible(false);
            settingsLabel.setManaged(false);
            pauseLabel.setVisible(false);
            pauseLabel.setManaged(false);
            volumeLabel.setVisible(false);
            volumeLabel.setManaged(false);
            fullScreenLabel.setVisible(false);
            fullScreenLabel.setManaged(false);
            if (!liveMode) {
                progressLabelHBox.setVisible(false);
                progressLabelHBox.setManaged(false);
                videoProgressBar.setVisible(false);
                videoProgressBar.setManaged(false);
            }
        } else {
            // 使用内嵌播放器，键盘快捷键事件绑定
            parent.addEventFilter(KeyEvent.KEY_PRESSED, evt -> {
                switch (evt.getCode()) {
                    case SPACE -> togglePause();
                    case ESCAPE -> {
                        if (isFullScreen()) {
                            toggleFullScreen();
                        }
                    }
                    case F -> toggleFullScreen();
                    case Z -> fillWindowToggleSwitch.setSelected(!fillWindowToggleSwitch.isSelected());
                    case RIGHT -> movePosition(true);
                    case LEFT -> movePosition(false);
                    case UP -> moveVolume(true);
                    case DOWN -> moveVolume(false);
                }
            });
        }
        // 鼠标移动事件处理
        parent.addEventHandler(MouseEvent.MOUSE_MOVED, evt -> setControlsVisible(true));
        parentChildren.add(0, playerPane);
        parent.requestFocus();
        setLoading(true);
    }

    protected void setControlsVisible(boolean flag) {
        Cursor cursor = playerPane.getCursor();

        if (controlPane.isVisible() != flag) {
            controlPane.setVisible(flag);
        }
        if (liveChannelDrawer != null) {
            liveChannelDrawer.setVisible(flag);
        }
        if (flag) {
            controlPaneHideTimer.restart();
            if (cursor == Cursor.NONE) {
                playerPane.setCursor(Cursor.DEFAULT);
            }
        } else {
            if (cursor != Cursor.NONE) {
                playerPane.setCursor(Cursor.NONE);
            }
        }
    }

    protected void setControlsAutoHide(boolean flag) {
        if (flag) {
            controlPaneHideTimer.restart();
        } else {
            setControlsVisible(true);
            controlPaneHideTimer.stop();
        }
    }

    protected void setLoading(float bufferCached) {
        loadingProgressLabel.setText(String.format("%.1f%%", bufferCached));
        if (bufferCached >= 100) {
            setLoading(false);

            return;
        }
        setLoading(true);
        if (!loadingProgressLabel.isVisible()) {
            loadingProgressLabel.setVisible(true);
        }
    }

    protected void setLoading(boolean loading) {
        isLoading.set(loading);
        if (loading) {
            setError(false);
        }
        if (!loading && loadingProgressLabel.isVisible() && !isError()) {
            loadingProgressLabel.setVisible(false);
        }
    }

    protected void setError(boolean flag) {
        isError.set(flag);
    }

    protected boolean isError() {
        return isError.get();
    }

    private void bindPlayerPaneWidth(DoubleExpression widthProp) {
        DoubleProperty prefWidthProperty = playerPane.prefWidthProperty();
        DoubleProperty maxWidthProperty = playerPane.maxWidthProperty();
        DoubleProperty minWidthProperty = playerPane.minWidthProperty();

        if (prefWidthProperty.isBound()) {
            prefWidthProperty.unbind();
        }
        prefWidthProperty.bind(widthProp);
        if (maxWidthProperty.isBound()) {
            maxWidthProperty.unbind();
        }
        maxWidthProperty.bind(widthProp);
        if (minWidthProperty.isBound()) {
            minWidthProperty.unbind();
        }
        minWidthProperty.bind(widthProp);
    }

    protected void postEnterFullScreen() {
        // 隐藏除播放器外的所有控件
        setOtherNodesVisibleWhenFullScreen(false);
        // 绑定播放器宽度与父窗口宽度一致
        bindPlayerPaneWidth(parent.widthProperty());
        fullScreenRunnable.run();
        parent.requestFocus();
    }

    protected void postExitFullScreen() {
        // 显示所有控件
        setOtherNodesVisibleWhenFullScreen(true);
        // 绑定非全屏下的播放器宽度
        bindPlayerPaneWidth(paneWidthProp);
        fullScreenExitRunnable.run();
    }

    private void setOtherNodesVisibleWhenFullScreen(boolean visible) {
        parent.getChildren().forEach(p -> {
            if (p != playerPane) {
                log.info("{} - set {}", p, visible);
                p.setVisible(visible);
            }
        });
    }

    protected boolean isLoading() {
        return isLoading.get();
    }

    protected void postTimeChanged(long newTime) {
        if (videoLength.get() > 0) {
            if (isLoading()) {
                setLoading(false);
            }
            if (config.getLiveMode()) {

                return;
            }
            if (!isVideoProgressBarUsing.get()) {
                videoProgressLabel.setText(formatProgressText(newTime));
                videoProgressBar.setProgress(newTime / (double) videoLength.get());
            }
        }
    }

    protected void postFinished() {
        stepForwardRunnable.run();
    }

    protected void postError() {
        SystemHelper.allowSleep();
        setError(true);
        setLoading(false);
    }

    protected void postBuffering(float newCache) {
        setLoading(newCache);
    }

    protected void postPaused() {
        SystemHelper.allowSleep();
        if (isLoading()) {
            setLoading(false);
        }
        pausedPlayButtonImageView.setVisible(true);
        pauseLabel.setGraphic(playIcon);
    }

    protected void postPlaying() {
        SystemHelper.preventSleep();
        if (isLoading()) {
            setLoading(false);
        }
        if (pauseLabel.getGraphic() != pauseIcon) {
            pauseLabel.setGraphic(pauseIcon);
        }
        if (pausedPlayButtonImageView.isVisible()) {
            pausedPlayButtonImageView.setVisible(false);
        }
    }

    protected void postLengthChanged(long newLength) {
        initProgress.getAndUpdate(val -> {
            if (val != -1) {
                setPlayTime(val);
            }

            return -1;
        });
        if (config.getLiveMode()) {

            return;
        }
        videoLength.set(newLength);
        videoProgressLengthLabel.setText(formatProgressText(newLength));
    }

    protected void postMovedPosition() {}

    protected void postMovedVolume(double newVolume) {
        volumeSlider.setValue(newVolume);
    }

    protected void postMuted() {
        volumeLabel.setGraphic(volumeOnIcon);
    }

    private String formatProgressText(long totalMilliseconds) {
        // 将毫秒转换为秒
        long totalSeconds = totalMilliseconds / 1000;

        // 计算小时、分钟和秒
        int hours = (int) (totalSeconds / 3600);
        int minutes = (int) ((totalSeconds % 3600) / 60);
        int seconds = (int) (totalSeconds % 60);

        // 格式化字符串
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void play(TVPlayBO tvPlayBO) {
        String url = tvPlayBO.getUrl();
        Map<String, String> headers = tvPlayBO.getHeaders();
        String videoTitle = tvPlayBO.getVideoTitle();
        Long progress = tvPlayBO.getProgress();

        if (tvPlayBO.isAdFiltered()) {
            showToast(I18nHelper.get(I18nKeys.VIDEO_INFO_AD_FILTERED));
        }
        doPlay(url, headers, videoTitle, progress);
        setVolume((int) volumeSlider.getValue());
    }

    public void play(int liveChannelGroupIdx, int liveChannelIdx, int liveChannelLineIdx) {
        LiveChannelGroup liveChannelGroup = liveChannelGroups.get(liveChannelGroupIdx);
        LiveChannel liveChannel = liveChannelGroup.getChannels().get(liveChannelIdx);
        LiveChannel.Line liveChannelLine = liveChannel.getLines().get(liveChannelLineIdx);

        play(liveChannelGroup, liveChannel, liveChannelLine);
    }

    private void play(
            LiveChannelGroup liveChannelGroup, LiveChannel liveChannel, LiveChannel.Line liveChannelLine
    ) {
        LiveChannel lastPlayingLiveChannel = playingLive.getLiveChannel();

        playingLive.setLiveChannelGroup(liveChannelGroup);
        playingLive.setLiveChannel(liveChannel);
        playingLive.setLiveChannelLine(liveChannelLine);
        liveChannelDrawer.select(liveChannelGroup, liveChannel);

        doPlay(liveChannelLine.getUrl(), Map.of(), liveChannel.getTitle(), null);
        setVolume((int) volumeSlider.getValue());
        if (lastPlayingLiveChannel != liveChannel) {
            // 切换频道时，显示banner（同一个频道切换线路时不显示）
            showLiveChannelBanner(liveChannel, liveChannelLine);
        }
        updateLiveChannelLinesHBox(lastPlayingLiveChannel, liveChannel, liveChannelLine);
    }

    private void updateLiveChannelLinesHBox(
            @Nullable LiveChannel lastPlayingLiveChannel, LiveChannel liveChannel, LiveChannel.Line liveChannelLine
    ) {
        if (lastPlayingLiveChannel != liveChannel) {
            liveChannelLinesWithPaginator.clear();
            if (liveChannel.getLines().size() > 1) {
                for (LiveChannel.Line line : liveChannel.getLines()) {
                    liveChannelLinesWithPaginator.addLine(line);
                }
            }
            liveChannelLinesWithPaginator.focus(liveChannelLine);
        }
    }

    private void showLiveChannelBanner(LiveChannel liveChannel, LiveChannel.Line liveChannelLine) {
        String channelTitle = liveChannel.getTitle();

        liveChannelBanner.setChannelInfo(channelTitle, liveChannelLine.getLogoUrl());
        liveChannelBanner.setCurrentProgram(null, null, null);
        liveChannelBanner.setNextProgram(null, null);
        if (epgServiceUrlProperty.get() != null) {
            fetchAndApplyEpgAsync(channelTitle);
        }
        liveChannelBanner.show();
    }

    private void fetchAndApplyEpgAsync(String channelTitle) {
        AsyncUtil.execute(() -> {
            EPG epg = null;
            LocalDateTime now = LocalDateTime.now();
            LocalTime nowTime;
            String epgStartTimeStr;
            String epgEndTimeStr;
            LocalTime epgStartTime;
            LocalTime epgEndTime;
            MutableTriple<String, String, String> currentProgramTitleAndStartTimeAndEndTimeTriple;
            MutablePair<String, String> nextProgramTitleAndStartTimeAndEndTime;
            EPG.Data epgData;

            try {
                epg = epgFetchingHandler.handle(epgServiceUrlProperty.get(), channelTitle, now.toLocalDate())
                        .get(4, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Platform.runLater(() -> ToastHelper.showException(e));
            } catch (ExecutionException | TimeoutException e) {
                log.warn(
                        "Exception while fetching epg, channelTitle={}, epgServiceUrl={}",
                        channelTitle,
                        epgServiceUrlProperty.get(),
                        e
                );
            }
            if (epg == null) {

                return;
            }
            nowTime = now.toLocalTime();
            currentProgramTitleAndStartTimeAndEndTimeTriple = MutableTriple.of(null, null, null);
            nextProgramTitleAndStartTimeAndEndTime = MutablePair.of(null, null);
            List<EPG.Data> data = epg.getEpgData();
            for (int i = 0; i < data.size(); i++) {
                epgData = data.get(i);
                epgStartTimeStr = epgData.getStart();
                epgEndTimeStr = epgData.getEnd();
                if (epgStartTimeStr == null || epgEndTimeStr == null) {
                    break;
                }
                try {
                    epgStartTime = LocalTime.parse(epgData.getStart(), LOCAL_TIME_FORMATTER);
                    epgEndTime = LocalTime.parse(epgData.getEnd(), LOCAL_TIME_FORMATTER);
                } catch (DateTimeParseException e) {
                    log.warn(
                            "Invalid epg time format, channelTitle={}, epgServiceUrl={}, epgData={}",
                            channelTitle, epgServiceUrlProperty.get(), epgData
                    );
                    break;
                }
                if (!nowTime.isBefore(epgStartTime) && nowTime.isBefore(epgEndTime)) {
                    currentProgramTitleAndStartTimeAndEndTimeTriple.setLeft(epgData.getTitle());
                    currentProgramTitleAndStartTimeAndEndTimeTriple.setMiddle(epgStartTimeStr);
                    currentProgramTitleAndStartTimeAndEndTimeTriple.setRight(epgEndTimeStr);
                    if (i < data.size() - 1) {
                        epgData = data.get(i + 1);
                        nextProgramTitleAndStartTimeAndEndTime.setLeft(epgData.getTitle());
                        nextProgramTitleAndStartTimeAndEndTime.setRight(epgData.getStart());
                    }
                    Platform.runLater(() -> {
                        liveChannelBanner.setCurrentProgram(
                                currentProgramTitleAndStartTimeAndEndTimeTriple.getLeft(),
                                currentProgramTitleAndStartTimeAndEndTimeTriple.getMiddle(),
                                currentProgramTitleAndStartTimeAndEndTimeTriple.getRight()
                        );
                        liveChannelBanner.setNextProgram(
                                nextProgramTitleAndStartTimeAndEndTime.getLeft(),
                                nextProgramTitleAndStartTimeAndEndTime.getRight()
                        );
                    });
                    break;
                }
            }
        });
    }

    /**
     * 实际进行播放所调用的方法
     * @return 是否成功播放
     */
    protected boolean doPlay(String url, Map<String, String> headers, String videoTitle, @Nullable Long progress) {
        if (destroyFlag) {

            return false;
        }
        setLoading(true);
        if (progress != null) {
            initProgress.set(Math.max(progress, -1));
        }
        setVideoTitle(videoTitle);
        if (!config.getLiveMode()) {
            if (subtitleSettingPopOver.isShowing()) {
                subtitleSettingPopOver.hide();
            }
            subtitleSettingPopOver.setMovieName(StringUtils.substringBetween(videoTitle, "《", "》"));
            subtitleSettingPopOver.resetSubtitleDelay();
        }

        return true;
    }

    /**
     * 创建播放器节点
     * 会在此基类中调用，子类无需调用
     * @return 播放器节点
     */
    protected abstract T createPlayerNode();

    /**
     * 设置字幕偏移
     *
     * @param delay 偏移量
     */
    protected void setSubtitleDelay(long delay) {}

    /**
     * 设置字幕
     * @param subtitleFile 字幕文件
     * @return 是否成功
     */
    protected boolean useSubtitle(File subtitleFile) { return false; }

    /**
     * 设置字幕可见性
     *
     * @param visible 是否可见
     */
    protected void setSubtitleVisible(boolean visible) {}

    /**
     * 设置（跳转）播放进度
     * @param time 播放进度
     */
    protected void setPlayTime(long time) {}

    /**
     * 移动播放进度一个单位
     * @param forward 是否向前
     */
    protected void movePosition(boolean forward) {}

    /**
     * 移动音量一个单位
     * @param forward 是否升高
     */
    protected void moveVolume(boolean forward) {}

    /**
     * 设置音量
     * @param volume 音量
     */
    protected void setVolume(int volume) {}

    /**
     * 静音切换
     */
    protected void toggleMute() {}

    /**
     * 获取当前是否静音
     */
    protected boolean isMute() { return false; }

    /**
     * 暂停切换
     */
    protected void togglePause() {}

    /**
     * 设置播放速度
     */
    protected void setRate(float rate) {}

    /**
     * 设置播放区域是否铺满窗口
     * @param fill 是否铺满
     */
    protected void setFillWindow(boolean fill) {}

    /**
     * stop后恢复播放
     */
    protected void play() {}

    /**
     * 切换全屏
     */
    protected void toggleFullScreen() {}

    /**
     * 获取当前是否全屏
     * @return 是否全屏
     */
    protected boolean isFullScreen() { return false; }

    /**
     * 获取当前是否可拖动进度
     * @return 是否可拖动进度
     */
    protected boolean isSeekable() { return false; }

    /**
     * 设置播放进度（按百分比）
     * @param positionPercent 播放进度
     */
    protected void setPositionPercent(float positionPercent) {}

    /**
     * 获取当前播放进度（允许阻塞）
     * @return 当前播放进度
     */
    public long getCurrentProgress() { return 0; }

    /**
     * 停止播放
     */
    public void stop() {}

    /**
     * 销毁播放器
     * 此类中的方法不会阻塞，但子类（比如VLC播放器）中覆盖的方法可能会阻塞
     */
    public void destroy() {
        if (destroyFlag) {

            return;
        }
        destroyFlag = true;
        controlPaneHideTimer.stop();
        volumePopOverHideTimer.stop();
        settingsPopOverHideTimer.stop();
        Platform.runLater(() -> {
            if (epgStage != null && epgStage.isShowing()) {
                epgStage.hide();
            }
            if (liveChannelBanner != null) {
                liveChannelBanner.destroy();
            }
        });
    }

    public void setOnStepBackward(Runnable runnable) {
        this.stepBackwardRunnable = runnable;
    }

    public void setOnStepForward(Runnable runnable) {
        this.stepForwardRunnable = runnable;
    }

    public void setOnFullScreen(Runnable runnable) {
        this.fullScreenRunnable = runnable;
    }

    public void setOnFullScreenExit(Runnable runnable) {
        this.fullScreenExitRunnable = runnable;
    }

    public void setVideoTitle(String videoTitle) {
        videoTitleLabel.setText(videoTitle);
    }

    public void showToast(String message) {
        toastPane.showToast(message);
    }

    public void setLiveChannelGroups(List<LiveChannelGroup> liveChannelGroups) {
        this.liveChannelGroups = liveChannelGroups;
        this.liveChannelDrawer.setLiveChannelGroups(liveChannelGroups);
    }

    public void setEpgServiceUrl(String epgServiceUrl) {
        epgServiceUrlProperty.set(epgServiceUrl);
    }

    /**
     * 获取当前正在播放的直播信息
     * @throws IllegalStateException 如果当前不是直播模式
     * @return 直播分组名称，频道名称，频道线路名称
     */
    @Nullable
    public Triple<String, String, String> getCurrentLiveInfo() {
        LiveChannelGroup group;
        LiveChannel channel;
        LiveChannel.Line line;

        if (!config.getLiveMode()) {

            throw new IllegalStateException("Not in live mode");
        }
        if (playingLive == null) {

            return null;
        }
        group = playingLive.getLiveChannelGroup();
        if (group == null) {

            return null;
        }
        channel = playingLive.getLiveChannel();
        line = playingLive.getLiveChannelLine();

        return Triple.of(
                group.getTitle(),
                channel == null ? null : channel.getTitle(),
                line == null ? null : line.getTitle()
        );
    }

    /**
     * 播放器配置
     */
    @Data
    @Builder
    public static class Config {

        /**
         * 直播模式
         */
        private Boolean liveMode;

        /**
         * 外部模式
         */
        private Boolean externalMode;
    }

    private static class LiveChannelBanner extends HBox {

        private final LogoPane logoPane = new LogoPane(60, 24);
        private final Label channelNameLabel = new Label();
        private final Label currentProgramLabel = new Label();
        private final Label programTimeLabel = new Label();
        private final Label nextProgramLabel = new Label();
        private final Label nextProgramTimeLabel = new Label();
        private final Timer hideTimer = new Timer(6000, evt -> setVisible(false));

        public LiveChannelBanner() {
            super();

            HBox epgInfoHBox;
            VBox currentProgramVBox;
            VBox nextProgramVBox;
            VBox programInfoVBox;

            // 基本样式设置
            setAlignment(Pos.CENTER);
            getStyleClass().add("player-live-channel-banner");

            // 样式
            channelNameLabel.getStyleClass().add("player-live-channel-banner-channel-name-label");
            currentProgramLabel.getStyleClass().add("player-live-channel-banner-program-label");
            programTimeLabel.getStyleClass().add("player-live-channel-banner-program-time-label");
            nextProgramLabel.getStyleClass().add("player-live-channel-banner-program-label");
            nextProgramTimeLabel.getStyleClass().add("player-live-channel-banner-program-time-label");

            // 节目信息垂直布局
            currentProgramVBox = new VBox(2, currentProgramLabel, programTimeLabel);
            nextProgramVBox = new VBox(2, nextProgramLabel, nextProgramTimeLabel);
            epgInfoHBox = new HBox(15, currentProgramVBox, nextProgramVBox);
            programInfoVBox = new VBox(5, channelNameLabel, epgInfoHBox);
            programInfoVBox.setAlignment(Pos.CENTER_LEFT);

            // 主布局组装
            getChildren().addAll(logoPane, programInfoVBox);
        }

        public void show() {
            setVisible(true);
            hideTimer.restart();
        }

        /**
         * 设置频道信息
         * @param name 名称
         * @param logoUrl LOGO地址
         */
        @SuppressWarnings("ConstantConditions")
        public void setChannelInfo(String name, @Nullable String logoUrl) {
            channelNameLabel.setText(name);
            logoPane.setTitleAndLogoUrl(name, logoUrl);
        }

        /**
         * 设置当前节目信息
         * @param title 标题
         * @param startTime 开始时间
         * @param endTime 结束时间
         */
        public void setCurrentProgram(@Nullable String title, @Nullable String startTime, @Nullable String endTime) {
            if (StringUtils.isBlank(title)) {
                title = I18nHelper.get(I18nKeys.LIVE_PLAYER_DEFAULT_PROGRAM);
            }
            if (StringUtils.isBlank(startTime)) {
                startTime = "--:--";
            }
            if (StringUtils.isBlank(endTime)) {
                endTime = "--:--";
            }
            programTimeLabel.setText(I18nHelper.getFormatted(
                    I18nKeys.LIVE_PLAYER_PLAY_TIME, startTime + " - " + endTime
            ));
            currentProgramLabel.setText(I18nHelper.getFormatted(I18nKeys.LIVE_PLAYER_CURRENT_PROGRAM, title));
        }

        /**
         * 设置下个节目信息
         * @param title 标题
         * @param startTime 开始时间
         */
        public void setNextProgram(@Nullable String title, @Nullable String startTime) {
            if (StringUtils.isBlank(title)) {
                title = I18nHelper.get(I18nKeys.LIVE_PLAYER_DEFAULT_PROGRAM);
            }
            if (StringUtils.isBlank(startTime)) {
                startTime = "--:--";
            }
            nextProgramLabel.setText(I18nHelper.getFormatted(I18nKeys.LIVE_PLAYER_NEXT_PROGRAM, title));
            nextProgramTimeLabel.setText(I18nHelper.getFormatted(I18nKeys.LIVE_PLAYER_START_TIME, startTime));
        }

        public void destroy() {
            hideTimer.stop();
        }
    }

    private static class LiveDrawer extends HBox {

        private final ListView<LiveChannelGroup> liveChannelGroupListView;
        private final LiveChannelGroupListViewCellFactory liveChannelGroupListViewCellFactory;
        private final ListView<LiveChannel> liveChannelListView;
        private final LiveChannelListViewCellFactory liveChannelListViewCellFactory;

        public LiveDrawer(LiveInfoBO selectedLive, LiveInfoBO playingLive, StackPane playerPane, BasePlayer<?> player) {
            super();
            HBox listViewHBox = new HBox();
            Button actionBtn = new Button();
            ObservableList<String> actionBtnStyleClasses = actionBtn.getStyleClass();
            FontIcon actionBtnRightIcon = FontIcon.of(FontAwesome.ANGLE_DOUBLE_RIGHT, 20, Color.WHITESMOKE);
            FontIcon actionBtnLeftIcon = FontIcon.of(FontAwesome.ANGLE_DOUBLE_LEFT, 20, Color.WHITESMOKE);
            DoubleProperty minHeightProp = minHeightProperty();
            DoubleProperty maxHeightProp = maxHeightProperty();
            DoubleProperty minWidthProp = minWidthProperty();
            DoubleProperty maxWidthProp = maxWidthProperty();
            ReadOnlyDoubleProperty playerPaneHeightProp = playerPane.heightProperty();
            ReadOnlyDoubleProperty playerPaneWidthProp = playerPane.widthProperty();
            ReadOnlyDoubleProperty listViewHBoxWidthProp = listViewHBox.widthProperty();
            ReadOnlyDoubleProperty actionBtnWidthProp = actionBtn.widthProperty();
            ObservableList<Node> liveDrawerChildren = getChildren();
            ObservableList<Node> listViewHBoxChildren = listViewHBox.getChildren();

            // 样式
            actionBtnStyleClasses.add("player-live-channel-list-toggle-button");
            actionBtn.setGraphic(actionBtnRightIcon);
            minHeightProp.bind(playerPaneHeightProp.divide(1.2));
            maxHeightProp.bind(playerPaneHeightProp.divide(1.2));
            minWidthProp.bind(actionBtnWidthProp);
            maxWidthProp.bind(actionBtnWidthProp);
            listViewHBox.setManaged(false);
            listViewHBox.setVisible(false);
            listViewHBox.minWidthProperty().bind(playerPaneWidthProp.divide(2.8));
            listViewHBox.maxWidthProperty().bind(playerPaneWidthProp.divide(2.8));
            liveDrawerChildren.add(listViewHBox);
            liveDrawerChildren.add(new StackPane(actionBtn));
            // 节目列表
            liveChannelListView = new ListView<>();
            liveChannelListViewCellFactory = new LiveChannelListViewCellFactory(selectedLive, playingLive, player);
            liveChannelListView.setCellFactory(liveChannelListViewCellFactory);
            liveChannelListView.getStyleClass().add("player-live-channel-list-view");
            liveChannelListView.setFocusTraversable(false);
            HBox.setHgrow(liveChannelListView, Priority.ALWAYS);
            // 节目分组列表
            liveChannelGroupListView = new ListView<>();
            liveChannelGroupListViewCellFactory = new LiveChannelGroupListViewCellFactory(
                    selectedLive, liveChannelListView, liveChannelListViewCellFactory.getLiveChannelAndTitleHBoxMap()
            );
            liveChannelGroupListView.setCellFactory(liveChannelGroupListViewCellFactory);
            liveChannelGroupListView.getStyleClass().add("player-live-channel-list-view");
            liveChannelGroupListView.setFocusTraversable(false);

            listViewHBoxChildren.add(liveChannelGroupListView);
            listViewHBoxChildren.add(liveChannelListView);

            // 展开/收起 按钮
            actionBtn.setFocusTraversable(false);
            actionBtn.setOnAction(evt -> {
                listViewHBox.setManaged(!listViewHBox.isManaged());
                listViewHBox.setVisible(!listViewHBox.isVisible());
                // 根据显示状态，更新Drawer的宽度
                minWidthProp.unbind();
                maxWidthProp.unbind();
                if (listViewHBox.isVisible()) {
                    minWidthProp.bind(listViewHBoxWidthProp.add(actionBtnWidthProp));
                    maxWidthProp.bind(listViewHBoxWidthProp.add(actionBtnWidthProp));
                    actionBtnStyleClasses.add("collapsed");
                    actionBtn.setGraphic(actionBtnLeftIcon);
                } else {
                    minWidthProp.bind(actionBtnWidthProp);
                    maxWidthProp.bind(actionBtnWidthProp);
                    actionBtnStyleClasses.remove("collapsed");
                    actionBtn.setGraphic(actionBtnRightIcon);
                }
            });
            actionBtn.setMinWidth(30);
            actionBtn.minHeightProperty().bind(actionBtn.widthProperty().multiply(2));
        }

        public void setLiveChannelGroups(List<LiveChannelGroup> liveChannelGroups) {
            ObservableList<LiveChannelGroup> items = liveChannelGroupListView.getItems();

            items.clear();
            items.addAll(liveChannelGroups);
        }

        public void select(LiveChannelGroup liveChannelGroup, LiveChannel liveChannel) {
            liveChannelGroupListViewCellFactory.select(liveChannelGroup, true);
            liveChannelListViewCellFactory.select(liveChannel);
        }
    }

    private static class LiveChannelGroupListViewCellFactory
            implements Callback<ListView<LiveChannelGroup>, ListCell<LiveChannelGroup>>
    {
        private final LiveInfoBO selectedLive;
        private final ListView<LiveChannel> liveChannelListView;
        private final Map<LiveChannelGroup, Label> liveChannelGroupAndTitleLabelMap;
        private final Map<LiveChannel, HBox> liveChannelAndTitleHBoxMap;

        public LiveChannelGroupListViewCellFactory(
                LiveInfoBO selectedLive,
                ListView<LiveChannel> liveChannelListView,
                Map<LiveChannel, HBox> liveChannelAndTitleHBoxMap
        ) {
            super();
            this.selectedLive = selectedLive;
            this.liveChannelListView = liveChannelListView;
            this.liveChannelAndTitleHBoxMap = liveChannelAndTitleHBoxMap;
            this.liveChannelGroupAndTitleLabelMap = new HashMap<>();
        }

        @Override
        public ListCell<LiveChannelGroup> call(ListView<LiveChannelGroup> liveChannelGroupListView) {
            return new ListCell<>() {

                @Override
                protected void updateItem(LiveChannelGroup liveChannelGroup, boolean empty) {
                    List<String> styleClasses;
                    Label titleLabel;
                    List<String> titleLabelStyleClass;
                    LiveChannelGroup selectedLiveChannelGroup;

                    super.updateItem(liveChannelGroup, empty);
                    styleClasses = getStyleClass();
                    if (!styleClasses.contains("player-live-channel-group-list-cell")) {
                        styleClasses.add("player-live-channel-group-list-cell");
                    }
                    setText(null);
                    if (empty) {
                        setGraphic(null);

                        return;
                    }
                    titleLabel = liveChannelGroupAndTitleLabelMap.get(liveChannelGroup);
                    if (titleLabel == null) {
                        titleLabel = new Label(liveChannelGroup.getTitle());
                        titleLabel.setAlignment(Pos.CENTER);
                        titleLabelStyleClass = titleLabel.getStyleClass();
                        titleLabel.maxWidthProperty().bind(liveChannelGroupListView.widthProperty().divide(1.3));
                        titleLabel.setOnMouseClicked(evt -> {
                            if (evt.getButton() != MouseButton.PRIMARY) {

                                return;
                            }
                            select(liveChannelGroup, false);
                        });
                        liveChannelGroupAndTitleLabelMap.put(liveChannelGroup, titleLabel);
                        selectedLiveChannelGroup = selectedLive.getLiveChannelGroup();
                        if (selectedLiveChannelGroup != null && liveChannelGroup == selectedLiveChannelGroup) {
                            titleLabelStyleClass.add("player-live-channel-list-view-title-label-focused");
                            setLiveChannels(liveChannelGroup.getChannels(), false);
                        } else {
                            titleLabelStyleClass.add("player-live-channel-list-view-title-label");
                        }
                    }
                    setGraphic(titleLabel);
                    setAlignment(Pos.CENTER);
                }
            };
        }

        /**
         * 选中节目分组
         * @param liveChannelGroup 被选中的节目分组
         * @param isPlaying 是否是播放选中。即是否为“上一集/下一集”切换时的选中（如果是，需要移除节目列表中上一个的节目标题高亮）
         */
        private void select(LiveChannelGroup liveChannelGroup, boolean isPlaying) {
            Label titleLabel = liveChannelGroupAndTitleLabelMap.get(liveChannelGroup);
            LiveChannelGroup lastLiveChannelGroup;
            List<String> titleLabelStyleClass;
            Label lastSelectedTitleLabel;
            List<String> lastSelectedTitleLabelStyleClass;

            lastLiveChannelGroup = selectedLive.getLiveChannelGroup();
            selectedLive.setLiveChannelGroup(liveChannelGroup);
            if (titleLabel == null) {

                return;
            }
            titleLabelStyleClass = titleLabel.getStyleClass();
            if (lastLiveChannelGroup == null) {
                titleLabelStyleClass.remove("player-live-channel-list-view-title-label");
                titleLabelStyleClass.add("player-live-channel-list-view-title-label-focused");
                setLiveChannels(liveChannelGroup.getChannels(), false);
            } else if (lastLiveChannelGroup != liveChannelGroup) {
                titleLabelStyleClass.remove("player-live-channel-list-view-title-label");
                titleLabelStyleClass.add("player-live-channel-list-view-title-label-focused");
                lastSelectedTitleLabel = liveChannelGroupAndTitleLabelMap.get(lastLiveChannelGroup);
                if (lastSelectedTitleLabel != null) {
                    lastSelectedTitleLabelStyleClass = lastSelectedTitleLabel.getStyleClass();
                    lastSelectedTitleLabelStyleClass.remove(
                            "player-live-channel-list-view-title-label-focused"
                    );
                    lastSelectedTitleLabelStyleClass.add(
                            "player-live-channel-list-view-title-label"
                    );
                }
                setLiveChannels(liveChannelGroup.getChannels(), isPlaying);
            }
        }

        /**
         * 设置节目列表
         * @param liveChannels 节目列表
         * @param clearLastHighlightStyle 是否移除上一个被选中的节目的高亮效果
         */
        private void setLiveChannels(List<LiveChannel> liveChannels, boolean clearLastHighlightStyle) {
            ObservableList<LiveChannel> items = liveChannelListView.getItems();
            LiveChannel lastSelectedLiveChannel;
            HBox lastLiveChannelTitleHBox;
            Label lastLiveChannelTitleLabel;
            List<String> lastLiveChannelTitleLabelStyleClass;

            items.clear();
            items.addAll(liveChannels);
            if (!clearLastHighlightStyle) {

                return;
            }
            lastSelectedLiveChannel = selectedLive.getLiveChannel();
            if (lastSelectedLiveChannel == null) {

                return;
            }
            lastLiveChannelTitleHBox = liveChannelAndTitleHBoxMap.get(lastSelectedLiveChannel);
            if (lastLiveChannelTitleHBox == null) {

                return;
            }
            lastLiveChannelTitleLabel = CastUtil.cast(CollectionUtil.getFirst(lastLiveChannelTitleHBox.getChildren()));
            if (lastLiveChannelTitleLabel == null) {

                return;
            }
            lastLiveChannelTitleLabelStyleClass = lastLiveChannelTitleLabel.getStyleClass();
            lastLiveChannelTitleLabelStyleClass.remove("player-live-channel-list-view-title-label-focused");
            lastLiveChannelTitleLabelStyleClass.add("player-live-channel-list-view-title-label");
        }
    }

    private static class LiveChannelListViewCellFactory
            implements Callback<ListView<LiveChannel>, ListCell<LiveChannel>>
    {
        private final LiveInfoBO selectedLive;
        private final LiveInfoBO playingLive;
        private final BasePlayer<?> player;
        private final Map<LiveChannel, BorderPane> liveChannelAndGraphicBorderPaneMap;
        @Getter
        private final Map<LiveChannel, HBox> liveChannelAndTitleHBoxMap;

        private final static double LOGO_IMAGE_SIZE = 50;
        private final static double LOGO_PLACE_HOLDER_SIZE = 35;
        private final static double LOGO_PLACE_HOLDER_FONT_SIZE = 18;
        private final static ImageView PLAYING_GIF_IMAGE_VIEW;

        static {
            PLAYING_GIF_IMAGE_VIEW = new ImageView(BaseResources.PLAYING_GIF);
            PLAYING_GIF_IMAGE_VIEW.setFitWidth(LOGO_PLACE_HOLDER_SIZE);
            PLAYING_GIF_IMAGE_VIEW.setFitHeight(LOGO_PLACE_HOLDER_SIZE);
            PLAYING_GIF_IMAGE_VIEW.setPreserveRatio(true);
        }

        public LiveChannelListViewCellFactory(LiveInfoBO selectedLive, LiveInfoBO playingLive, BasePlayer<?> player) {
            super();
            this.selectedLive = selectedLive;
            this.playingLive = playingLive;
            this.player = player;
            this.liveChannelAndGraphicBorderPaneMap = new HashMap<>();
            this.liveChannelAndTitleHBoxMap = new HashMap<>();
        }

        @Override
        @SuppressWarnings("ConstantConditions")
        public ListCell<LiveChannel> call(ListView<LiveChannel> liveChannelListView) {
            return new ListCell<>() {
                @Override
                protected void updateItem(LiveChannel liveChannel, boolean empty) {
                    List<String> styleClasses;
                    BorderPane graphicBorderPane;
                    Label titleLabel;
                    List<String> titleLabelStyleClass;
                    HBox titleHBox;
                    LiveChannel selectedLiveChannel;
                    List<Node> titleHBoxChildren;
                    String logoUrl;
                    LogoPane logoPane;

                    super.updateItem(liveChannel, empty);
                    styleClasses = getStyleClass();
                    if (!styleClasses.contains("player-live-channel-list-cell")) {
                        styleClasses.add("player-live-channel-list-cell");
                    }
                    setText(null);
                    if (empty) {
                        setGraphic(null);

                        return;
                    }
                    graphicBorderPane = ObjectUtil.defaultIfNull(
                            liveChannelAndGraphicBorderPaneMap.get(liveChannel), () -> new BorderPane()
                    );
                    if (graphicBorderPane.getCenter() == null) {
                        // 初始化Graphic
                        liveChannelAndGraphicBorderPaneMap.put(liveChannel, graphicBorderPane);
                        titleLabel = new Label(liveChannel.getTitle());
                        titleLabelStyleClass = titleLabel.getStyleClass();
                        titleLabel.maxWidthProperty().bind(liveChannelListView.widthProperty().divide(1.3));
                        titleLabel.setOnMousePressed(evt -> {
                            LiveChannelGroup liveChannelGroup;

                            if (evt.getButton() != MouseButton.PRIMARY || playingLive.getLiveChannel() == liveChannel) {

                                return;
                            }
                            liveChannelGroup = selectedLive.getLiveChannelGroup();
                            player.play(liveChannelGroup, liveChannel, liveChannel.getLines().get(0));
                        });
                        titleHBox = new HBox(titleLabel);
                        titleHBox.setAlignment(Pos.CENTER_LEFT);
                        liveChannelAndTitleHBoxMap.put(liveChannel, titleHBox);
                        selectedLiveChannel = selectedLive.getLiveChannel();
                        if (selectedLiveChannel != null && liveChannel == selectedLiveChannel) {
                            titleLabelStyleClass.add("player-live-channel-list-view-title-label-focused");
                            titleHBoxChildren = titleHBox.getChildren();
                            if (!titleHBoxChildren.contains(PLAYING_GIF_IMAGE_VIEW)) {
                                titleHBoxChildren.add(1, PLAYING_GIF_IMAGE_VIEW);
                            }
                        } else {
                            titleLabelStyleClass.add("player-live-channel-list-view-title-label");
                        }
                        graphicBorderPane.setCenter(titleHBox);
                        // 台标
                        logoUrl = liveChannel.getLines()
                                .stream()
                                .map(LiveChannel.Line::getLogoUrl)
                                .filter(StringUtils::isNotBlank)
                                .findFirst()
                                .orElse(null);
                        // 台标
                        logoPane = new LogoPane(
                                LOGO_IMAGE_SIZE, LOGO_PLACE_HOLDER_SIZE, LOGO_PLACE_HOLDER_FONT_SIZE
                        );
                        logoPane.setTitleAndLogoUrl(liveChannel.getTitle(), logoUrl);
                        graphicBorderPane.setLeft(logoPane);
                    }
                    setGraphic(graphicBorderPane);
                    setAlignment(Pos.CENTER);
                }
            };
        }

        private void select(LiveChannel liveChannel) {
            HBox titleHBox = liveChannelAndTitleHBoxMap.get(liveChannel);
            Label titleLabel;
            LiveChannel lastLiveChannel;
            List<String> titleLabelStyleClass;
            List<Node> titleHBoxChildren;
            HBox lastSelectedTitleHBox;
            Label lastSelectedTitleLabel;
            List<String> lastSelectedTitleLabelStyleClass;
            List<Node> lastSelectedTitleHBoxChildren;

            lastLiveChannel = selectedLive.getLiveChannel();
            selectedLive.setLiveChannel(liveChannel);
            if (titleHBox == null) {

                return;
            }
            titleLabel = CastUtil.cast(CollectionUtil.getFirst(titleHBox.getChildren()));
            if (titleLabel == null) {

                return;
            }
            titleLabelStyleClass = titleLabel.getStyleClass();
            titleHBoxChildren = titleHBox.getChildren();
            if (lastLiveChannel == null) {
                titleLabelStyleClass.remove("player-live-channel-list-view-title-label");
                titleLabelStyleClass.add("player-live-channel-list-view-title-label-focused");
                if (!titleHBoxChildren.contains(PLAYING_GIF_IMAGE_VIEW)) {
                    titleHBoxChildren.add(1, PLAYING_GIF_IMAGE_VIEW);
                }
            } else if (lastLiveChannel != liveChannel) {
                titleLabelStyleClass.remove("player-live-channel-list-view-title-label");
                titleLabelStyleClass.add("player-live-channel-list-view-title-label-focused");
                lastSelectedTitleHBox = liveChannelAndTitleHBoxMap.get(lastLiveChannel);
                lastSelectedTitleLabel = CastUtil.cast(CollectionUtil.getFirst(lastSelectedTitleHBox.getChildren()));
                if (lastSelectedTitleLabel == null) {

                    return;
                }
                lastSelectedTitleLabelStyleClass = lastSelectedTitleLabel.getStyleClass();
                lastSelectedTitleLabelStyleClass.remove(
                        "player-live-channel-list-view-title-label-focused"
                );
                lastSelectedTitleLabelStyleClass.add(
                        "player-live-channel-list-view-title-label"
                );
                lastSelectedTitleHBoxChildren = lastSelectedTitleHBox.getChildren();
                lastSelectedTitleHBoxChildren.remove(PLAYING_GIF_IMAGE_VIEW);
                if (!titleHBoxChildren.contains(PLAYING_GIF_IMAGE_VIEW)) {
                    titleHBoxChildren.add(1, PLAYING_GIF_IMAGE_VIEW);
                }
            }
        }
    }

    @Data
    private static class LiveInfoBO {

        private LiveChannelGroup liveChannelGroup;

        private LiveChannel liveChannel;

        private LiveChannel.Line liveChannelLine;
    }

    public static BasePlayer<?> createPlayer(Pane parentPane) {

        return createPlayer(parentPane, null);
    }

    public static BasePlayer<?> createPlayer(Pane parentPane, @Nullable Config config) {
        if (config == null) {
            config = Config.builder().liveMode(false).externalMode(false).build();
        }

        return switch (ConfigHelper.getPlayerType()) {
            case VLC -> new VLCPlayer(parentPane, config);
            case MPV_EXTERNAL -> {
                if (!BooleanUtils.toBoolean(config.getExternalMode())) {
                    config.setExternalMode(true);
                }
                yield new MPVExternalPlayer(parentPane, config);
            }
        };
    }
}
