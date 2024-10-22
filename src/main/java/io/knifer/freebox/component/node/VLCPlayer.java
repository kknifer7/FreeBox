package io.knifer.freebox.component.node;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.WindowHelper;
import javafx.application.Platform;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableList;
import javafx.event.Event;
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
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.ToggleSwitch;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.fullscreen.JavaFXFullScreenStrategy;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * VLC播放器自定义组件
 * PS：我知道这里的代码很乱，没有注释，因为我没指望谁有心思动这个类。如果你有什么想法，直接告诉我。
 *
 * @author Knifer
 */
@Slf4j
@Getter
public class VLCPlayer {

    private final Stage stage;
    private final Scene scene;
    private final AnchorPane controlPane;
    private final Timer controlPaneHideTimer;
    private final EmbeddedMediaPlayer mediaPlayer;
    private final ImageView videoImageView;
    private final ProgressIndicator loadingProgressIndicator;
    private final Label pauseLabel;
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
    private final Label settingsLabel;
    private final ProgressBar videoProgressBar;
    private final Label videoProgressLabel;
    private final Label videoProgressSplitLabel;
    private final Label videoProgressLengthLabel;
    private final Label fullScreenLabel;
    private final Label videoTitleLabel;
    private final StackPane playerPane;
    private final FontIcon pauseIcon = FontIcon.of(FontAwesome.PAUSE, 32, Color.WHITE);
    private final FontIcon playIcon = FontIcon.of(FontAwesome.PLAY, 32, Color.WHITE);
    private final FontIcon stepForwardIcon = FontIcon.of(FontAwesome.STEP_FORWARD, 32, Color.WHITE);
    private final FontIcon volumeUpIcon = FontIcon.of(FontAwesome.VOLUME_UP, 32, Color.WHITE);
    private final FontIcon volumeOffIcon = FontIcon.of(FontAwesome.VOLUME_OFF, 32, Color.WHITE);
    private final FontIcon fullScreenIcon = FontIcon.of(FontAwesome.ARROWS_ALT, 32, Color.WHITE);
    private final FontIcon settingsIcon = FontIcon.of(FontAwesome.SLIDERS, 32, Color.WHITE);
    private final AtomicLong videoLength = new AtomicLong(-1);
    private final AtomicLong initProgress = new AtomicLong(-1);
    private final AtomicBoolean isVideoProgressBarUsing = new AtomicBoolean(false);
    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);

    private Runnable stepForwardRunnable;

    public VLCPlayer(HBox parent) {
        ObservableList<Node> parentChildren = parent.getChildren();
        ReadOnlyDoubleProperty parentWidthProp = parent.widthProperty();
        DoubleBinding paneWidthProp = parentWidthProp.multiply(0.8);
        ReadOnlyDoubleProperty parentHeightProp = parent.heightProperty();
        List<Node> paneChildren;
        PopOver volumePopOver;
        Timer volumePopOverHideTimer;
        Label rateSettingTitleLabel;
        HBox rateSettingRadioButtonHBox;
        HBox rateSettingHBox;
        VBox settingsPopOverInnerVBox;
        PopOver settingsPopOver;
        Timer settingsPopOverHideTimer;
        HBox progressLabelHBox;
        HBox leftToolBarHbox;
        HBox rightToolBarHbox;
        AnchorPane controlBottomAnchorPane;
        AnchorPane controlTopAnchorPane;

        stage = WindowHelper.getStage(parent);
        scene = stage.getScene();
        playerPane = new StackPane();
        stage.setFullScreenExitHint(StringUtils.EMPTY);
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        mediaPlayer = new MediaPlayerFactory().mediaPlayers().newEmbeddedMediaPlayer();
        mediaPlayer.fullScreen().strategy(new JavaFXFullScreenStrategy(stage){
            @Override
            public void onBeforeEnterFullScreen() {
                // 隐藏除播放器外的所有控件
                setOtherNodesVisible(false);
                // 绑定播放器宽度与父窗口宽度一致
                bindPlayerPaneWidth(parentWidthProp);
                parent.requestFocus();
            }

            @Override
            public void onAfterExitFullScreen() {
                // 显示所有控件
                setOtherNodesVisible(true);
                // 绑定非全屏下的播放器宽度
                bindPlayerPaneWidth(paneWidthProp);
            }

            private void setOtherNodesVisible(boolean visible) {
                parentChildren.forEach(p -> {
                    if (p != playerPane) {
                        p.setVisible(visible);
                    }
                });
            }
        });
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void mediaPlayerReady(MediaPlayer mediaPlayer) {
                setLoading(false);
            }

            @Override
            public void buffering(MediaPlayer mediaPlayer, float newCache) {
                if (!isLoading()) {
                    setLoading(true);
                }
            }

            @Override
            public void paused(MediaPlayer mediaPlayer) {
                Platform.runLater(() -> {
                    if (isLoading()) {
                        setLoading(false);
                    }
                    pauseLabel.setGraphic(playIcon);
                });
            }

            @Override
            public void playing(MediaPlayer mediaPlayer) {
                Platform.runLater(() -> {
                    if (isLoading()) {
                        setLoading(false);
                    }
                    pauseLabel.setGraphic(pauseIcon);
                });
            }

            @Override
            public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
                long length = mediaPlayer.status().length();

                initProgress.getAndUpdate(val -> {
                    if (val == -1) {
                        return -1;
                    }
                    Platform.runLater(() -> mediaPlayer.controls().setPosition((float) (val / (double) length)));

                    return -1;
                });
                Platform.runLater(() -> {
                    videoLength.set(length);
                    videoProgressLengthLabel.setText(formatProgressText(length));
                });
            }

            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                if (videoLength.get() > 0) {
                    Platform.runLater(() -> {
                        if (!isVideoProgressBarUsing.get()) {
                            videoProgressLabel.setText(formatProgressText(newTime));
                            videoProgressBar.setProgress(newTime / (double) videoLength.get());
                        }
                        if (isLoading()) {
                            setLoading(false);
                        }
                    });
                }
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                stepForwardRunnable.run();
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                log.error("VLCPlayer error");
            }
        });
        videoImageView = new ImageView();
        videoImageView.setPreserveRatio(true);
        videoImageView.fitWidthProperty().bind(playerPane.widthProperty());
        videoImageView.fitHeightProperty().bind(playerPane.heightProperty());
        mediaPlayer.videoSurface().set(new ImageViewVideoSurface(videoImageView));
        loadingProgressIndicator = new ProgressIndicator();
        loadingProgressIndicator.visibleProperty().bind(isLoading);
        // 暂停设置
        pauseLabel = new Label();
        pauseLabel.setGraphic(pauseIcon);
        pauseLabel.getStyleClass().add("vlc-player-control-label");
        pauseLabel.setOnMouseClicked(evt -> changePlayStatus());
        // 下一集
        stepForwardRunnable = () -> {};
        stepForwardLabel = new Label();
        stepForwardLabel.setGraphic(stepForwardIcon);
        stepForwardLabel.getStyleClass().add("vlc-player-control-label");
        stepForwardLabel.setOnMouseClicked(evt -> stepForwardRunnable.run());
        // 音量设置
        volumeLabel = new Label();
        volumeLabel.setGraphic(volumeUpIcon);
        volumeLabel.getStyleClass().add("vlc-player-control-label");
        volumeLabel.setOnMouseClicked(evt -> {
            volumeLabel.setGraphic(mediaPlayer.audio().isMute() ? volumeUpIcon : volumeOffIcon);
            mediaPlayer.audio().mute();
        });
        volumeSlider = new Slider(0, 100, 100);
        volumeSlider.setOrientation(Orientation.VERTICAL);
        volumePopOver = new PopOver(volumeSlider);
        volumePopOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
        volumePopOver.getStyleClass().add("vlc-player-pop-over");
        volumePopOver.setDetachable(false);
        volumePopOverHideTimer = new Timer(1000, evt -> volumePopOver.hide());
        volumePopOver.addEventFilter(MouseEvent.ANY, evt -> {
            if (evt.getEventType() == MouseEvent.MOUSE_EXITED) {
                volumePopOverHideTimer.stop();
            } else {
                volumePopOverHideTimer.restart();
            }
        });
        volumeSlider.valueProperty().addListener((ob, oldVal, newVal) -> {
            mediaPlayer.audio().setVolume(newVal.intValue());
            if (mediaPlayer.audio().isMute()) {
                mediaPlayer.audio().mute();
                volumeLabel.setGraphic(volumeUpIcon);
            }
        });
        volumeLabel.setOnMouseEntered(evt -> {
            volumePopOverHideTimer.restart();
            if (!volumePopOver.isShowing()) {
                volumePopOver.show(volumeLabel);
            }
        });
        settingsLabel = new Label();
        settingsLabel.getStyleClass().add("vlc-player-control-label");
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
            mediaPlayer.controls().setRate((float) newValue.getUserData())
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
        videoImageView.preserveRatioProperty().bind(fillWindowToggleSwitch.selectedProperty().not());
        settingsPopOver = new PopOver();
        settingsPopOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
        settingsPopOver.getStyleClass().add("vlc-player-pop-over");
        settingsPopOver.setDetachable(false);
        settingsPopOverInnerVBox = new VBox(fillWindowToggleSwitch, rateSettingHBox);
        settingsPopOverInnerVBox.setSpacing(10.0);
        settingsPopOver.setContentNode(settingsPopOverInnerVBox);
        settingsPopOverHideTimer = new Timer(1000, evt -> settingsPopOver.hide());
        settingsPopOver.addEventFilter(MouseEvent.ANY, evt -> {
            if (evt.getEventType() == MouseEvent.MOUSE_EXITED) {
                settingsPopOverHideTimer.stop();
            } else {
                settingsPopOverHideTimer.restart();
            }
        });
        settingsLabel.setOnMouseEntered(evt -> {
            settingsPopOverHideTimer.restart();
            if (!settingsPopOver.isShowing()) {
                settingsPopOver.show(settingsLabel);
            }
        });
        // 进度条组件
        videoProgressBar = new ProgressBar(0);
        videoProgressLabel = new Label("00:00:00");
        videoProgressLabel.getStyleClass().add("vlc-player-progress-label");
        videoProgressBar.setOnMousePressed(evt -> {
            double newProgress;

            if (!mediaPlayer.status().isPlayable() || !mediaPlayer.status().isSeekable()) {
                return;
            }
            isVideoProgressBarUsing.set(true);
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

            if (!mediaPlayer.status().isPlayable() || !mediaPlayer.status().isSeekable()) {
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
            if (!mediaPlayer.status().isPlayable() || !mediaPlayer.status().isSeekable()) {
                return;
            }
            setLoading(true);
            mediaPlayer.controls().setPosition(((float) videoProgressBar.getProgress()));
            isVideoProgressBarUsing.set(false);
        });
        videoProgressBar.disableProperty().bind(isLoading);
        videoProgressSplitLabel = new Label("/");
        videoProgressSplitLabel.getStyleClass().add("vlc-player-progress-label");
        videoProgressLengthLabel = new Label("-:-:-");
        videoProgressLengthLabel.getStyleClass().add("vlc-player-progress-label");
        progressLabelHBox = new HBox(videoProgressLabel, videoProgressSplitLabel, videoProgressLengthLabel);
        progressLabelHBox.setSpacing(5);
        progressLabelHBox.setAlignment(Pos.CENTER);
        // 铺满、全屏组件
        fullScreenLabel = new Label();
        fullScreenLabel.getStyleClass().add("vlc-player-control-label");
        fullScreenLabel.setGraphic(fullScreenIcon);
        fullScreenLabel.setOnMouseClicked(evt -> mediaPlayer.fullScreen().toggle());
        leftToolBarHbox = new HBox(pauseLabel, stepForwardLabel, volumeLabel, settingsLabel, progressLabelHBox);
        leftToolBarHbox.setSpacing(20);
        leftToolBarHbox.setAlignment(Pos.CENTER);
        rightToolBarHbox = new HBox(fullScreenLabel);
        rightToolBarHbox.setSpacing(20);
        controlBottomAnchorPane = new AnchorPane(leftToolBarHbox, videoProgressBar, rightToolBarHbox);
        controlBottomAnchorPane.getStyleClass().add("vlc-player-anchor-pane");
        controlBottomAnchorPane.setOnMouseClicked(Event::consume);
        AnchorPane.setLeftAnchor(leftToolBarHbox, 10.0);
        AnchorPane.setTopAnchor(leftToolBarHbox, 10.0);
        AnchorPane.setBottomAnchor(leftToolBarHbox, 10.0);
        AnchorPane.setLeftAnchor(videoProgressBar, 490.0);
        AnchorPane.setRightAnchor(videoProgressBar, 70.0);
        AnchorPane.setTopAnchor(videoProgressBar, 10.0);
        AnchorPane.setBottomAnchor(videoProgressBar, 10.0);
        AnchorPane.setRightAnchor(rightToolBarHbox, 10.0);
        AnchorPane.setTopAnchor(rightToolBarHbox, 10.0);
        AnchorPane.setBottomAnchor(rightToolBarHbox, 10.0);
        // 顶端标题
        videoTitleLabel = new Label();
        videoTitleLabel.getStyleClass().add("vlc-player-title");
        controlTopAnchorPane = new AnchorPane(videoTitleLabel);
        controlTopAnchorPane.getStyleClass().add("vlc-player-anchor-pane");
        controlTopAnchorPane.setOnMouseClicked(Event::consume);
        AnchorPane.setLeftAnchor(videoTitleLabel, 10.0);
        AnchorPane.setRightAnchor(videoTitleLabel, 10.0);
        AnchorPane.setTopAnchor(videoTitleLabel, 10.0);
        AnchorPane.setBottomAnchor(videoTitleLabel, 10.0);
        // 摆放布局组件
        controlPane = new AnchorPane(controlBottomAnchorPane, controlTopAnchorPane);
        AnchorPane.setLeftAnchor(controlBottomAnchorPane, 0.0);
        AnchorPane.setRightAnchor(controlBottomAnchorPane, 0.0);
        AnchorPane.setBottomAnchor(controlBottomAnchorPane, 0.0);
        AnchorPane.setLeftAnchor(controlTopAnchorPane, 0.0);
        AnchorPane.setRightAnchor(controlTopAnchorPane, 0.0);
        AnchorPane.setTopAnchor(controlTopAnchorPane, 0.0);
        paneChildren = playerPane.getChildren();
        paneChildren.add(videoImageView);
        paneChildren.add(controlPane);
        paneChildren.add(loadingProgressIndicator);
        playerPane.getStyleClass().add("vlc-player");
        bindPlayerPaneWidth(paneWidthProp);
        playerPane.prefHeightProperty().bind(parentHeightProp);
        playerPane.minHeightProperty().bind(parentHeightProp);
        playerPane.maxHeightProperty().bind(parentHeightProp);
        playerPane.setOnMouseClicked(evt -> {
            if (evt.getClickCount() == 1) {
                changePlayStatus();
            } else {
                changePlayStatus();
                mediaPlayer.fullScreen().toggle();
            }
        });
        // 键盘快捷键事件绑定
        parent.addEventFilter(KeyEvent.KEY_PRESSED, evt -> {
            switch (evt.getCode()) {
                case SPACE -> changePlayStatus();
                case ESCAPE -> {
                    if (mediaPlayer.fullScreen().isFullScreen()) {
                        mediaPlayer.fullScreen().toggle();
                    }
                }
                case F -> mediaPlayer.fullScreen().toggle();
                case Z -> fillWindowToggleSwitch.setSelected(!fillWindowToggleSwitch.isSelected());
            }
        });
        // 鼠标移动事件处理
        controlPaneHideTimer = new Timer(2000, evt -> setControlsVisible(false));
        parent.addEventFilter(MouseEvent.MOUSE_MOVED, evt -> setControlsVisible(true));
        parentChildren.add(0, playerPane);
        parent.requestFocus();
        setLoading(true);
    }

    private void setControlsVisible(boolean flag) {
        Cursor cursor = scene.getCursor();

        if (controlPane.isVisible() != flag) {
            controlPane.setVisible(flag);
        }
        if (flag) {
            controlPaneHideTimer.restart();
            if (cursor == Cursor.NONE) {
                scene.setCursor(Cursor.DEFAULT);
            }
        } else {
            if (cursor != Cursor.NONE) {
                scene.setCursor(Cursor.NONE);
            }
        }
    }

    private void setLoading(boolean loading) {
        isLoading.set(loading);
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

    public void play(String url, String videoTitle, @Nullable Long progress) {
        setLoading(true);
        if (progress != null) {
            initProgress.set(Math.max(progress, -1));
        }
        videoTitleLabel.setText(videoTitle);
        mediaPlayer.media().play(url);
        mediaPlayer.audio().setVolume((int) volumeSlider.getValue());
    }

    public void changePlayStatus() {
        if (!mediaPlayer.status().canPause()) {
            return;
        }
        setLoading(true);
        mediaPlayer.controls().pause();
    }

    private boolean isLoading() {
        return isLoading.get();
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

    public void stop() {
        State playerState;

        setLoading(false);
        playerState = mediaPlayer.status().state();
        if (playerState != State.STOPPED && playerState != State.ENDED) {
            mediaPlayer.controls().stop();
        }
    }

    public void setOnStepForward(Runnable runnable) {
        this.stepForwardRunnable = runnable;
    }

    public void destroy() {
        mediaPlayer.release();
    }
}
