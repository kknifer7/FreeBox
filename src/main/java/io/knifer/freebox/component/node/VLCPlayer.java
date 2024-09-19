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
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.PopOver;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.fullscreen.JavaFXFullScreenStrategy;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

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

    private final EmbeddedMediaPlayer mediaPlayer;
    private final ImageView videoImageView;
    private final ProgressIndicator loadingProgressIndicator;
    private final Label pauseLabel;
    private final Slider volumeSlider;
    private final Label volumeLabel;
    private final ToggleGroup rateSettingToggleGroup;
    private final RadioButton rate0_5SettingRadioButton;
    private final RadioButton rate1SettingRadioButton;
    private final RadioButton rate1_25SettingRadioButton;
    private final RadioButton rate1_5SettingRadioButton;
    private final RadioButton rate2SettingRadioButton;
    private final Label settingsLabel;
    private final ProgressBar videoProgressBar;
    private final Label videoProgressLabel;
    private final Label videoProgressSplitLabel;
    private final Label videoProgressLengthLabel;
    private final Label fullScreenLabel;
    private final Label fillWindowLabel;
    private final StackPane playerPane;
    private final FontIcon pauseIcon = FontIcon.of(FontAwesome.PAUSE, 32, Color.WHITE);
    private final FontIcon playIcon = FontIcon.of(FontAwesome.PLAY, 32, Color.WHITE);
    private final FontIcon volumeUpIcon = FontIcon.of(FontAwesome.VOLUME_UP, 32, Color.WHITE);
    private final FontIcon volumeOffIcon = FontIcon.of(FontAwesome.VOLUME_OFF, 32, Color.WHITE);
    private final FontIcon fullScreenIcon = FontIcon.of(FontAwesome.ARROWS_ALT, 32, Color.WHITE);
    private final FontIcon fillWindowIcon = FontIcon.of(FontAwesome.WINDOW_MAXIMIZE, 32, Color.WHITE);
    private final FontIcon settingsIcon = FontIcon.of(FontAwesome.SLIDERS, 32, Color.WHITE);
    private final AtomicLong videoLength = new AtomicLong(-1);
    private final AtomicBoolean isVideoProgressBarUsing = new AtomicBoolean(false);
    private final BooleanProperty isLoading = new SimpleBooleanProperty(false);

    public VLCPlayer(HBox parent) {
        ObservableList<Node> parentChildren = parent.getChildren();
        ReadOnlyDoubleProperty parentWidthProp = parent.widthProperty();
        DoubleBinding paneWidthProp = parentWidthProp.multiply(0.8);
        ReadOnlyDoubleProperty parentHeightProp = parent.heightProperty();
        Stage stage = WindowHelper.getStage(parent);
        List<Node> paneChildren;
        AnchorPane controlInnerAnchorPane;
        PopOver volumePopOver;
        Timer volumePopOverHideTimer;
        Label rateSettingTitleLabel;
        HBox rateSettingRadioButtonHBox;
        HBox rateSettingHBox;
        PopOver settingsPopOver;
        Timer settingsPopOverHideTimer;
        HBox progressLabelHBox;
        HBox leftToolBarHbox;
        HBox rightToolBarHbox;
        StackPane controlPane;

        playerPane = new StackPane();
        stage.setFullScreenExitHint(StringUtils.EMPTY);
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        mediaPlayer = new MediaPlayerFactory().mediaPlayers().newEmbeddedMediaPlayer();
        mediaPlayer.fullScreen().strategy(new JavaFXFullScreenStrategy(stage){
            @Override
            public void onBeforeEnterFullScreen() {
                // 隐藏除播放器外的所有控件
                setBorderPaneChildrenVisible(false);
                // 绑定播放器宽度与父窗口宽度一致
                bindPlayerPaneWidth(parentWidthProp);
            }

            @Override
            public void onAfterExitFullScreen() {
                // 显示所有控件
                setBorderPaneChildrenVisible(true);
                // 绑定非全屏下的播放器宽度
                bindPlayerPaneWidth(paneWidthProp);
            }

            private void setBorderPaneChildrenVisible(boolean visible) {
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
                if (isLoading() && newCache >= 1) {
                    setLoading(false);
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
                    });
                }
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
        settingsPopOver = new PopOver();
        settingsPopOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_CENTER);
        settingsPopOver.getStyleClass().add("vlc-player-pop-over");
        settingsPopOver.setDetachable(false);
        settingsPopOver.setContentNode(rateSettingHBox);
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
        fillWindowLabel = new Label();
        fillWindowLabel.getStyleClass().add("vlc-player-control-label");
        fillWindowLabel.setGraphic(fillWindowIcon);
        fillWindowLabel.setOnMouseClicked(evt -> videoImageView.setPreserveRatio(!videoImageView.isPreserveRatio()));
        leftToolBarHbox = new HBox(pauseLabel, volumeLabel, settingsLabel, progressLabelHBox);
        leftToolBarHbox.setSpacing(20);
        leftToolBarHbox.setAlignment(Pos.CENTER);
        rightToolBarHbox = new HBox(fillWindowLabel, fullScreenLabel);
        rightToolBarHbox.setSpacing(20);
        controlInnerAnchorPane = new AnchorPane(leftToolBarHbox, videoProgressBar, rightToolBarHbox);
        controlInnerAnchorPane.getStyleClass().add("vlc-player-anchor-pane");
        controlInnerAnchorPane.setOnMouseClicked(Event::consume);
        AnchorPane.setLeftAnchor(leftToolBarHbox, 10.0);
        AnchorPane.setTopAnchor(leftToolBarHbox, 10.0);
        AnchorPane.setBottomAnchor(leftToolBarHbox, 10.0);
        AnchorPane.setLeftAnchor(videoProgressBar, 420.0);
        AnchorPane.setRightAnchor(videoProgressBar, 140.0);
        AnchorPane.setTopAnchor(videoProgressBar, 10.0);
        AnchorPane.setBottomAnchor(videoProgressBar, 10.0);
        AnchorPane.setRightAnchor(rightToolBarHbox, 10.0);
        AnchorPane.setTopAnchor(rightToolBarHbox, 10.0);
        AnchorPane.setBottomAnchor(rightToolBarHbox, 10.0);
        controlPane = new StackPane(controlInnerAnchorPane);
        controlPane.setAlignment(Pos.BOTTOM_CENTER);
        paneChildren = playerPane.getChildren();
        paneChildren.add(videoImageView);
        paneChildren.add(controlPane);
        paneChildren.add(loadingProgressIndicator);
        playerPane.setStyle("-fx-background-color: black;");
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
        parent.addEventFilter(KeyEvent.KEY_PRESSED, evt -> {
            switch (evt.getCode()) {
                case SPACE -> changePlayStatus();
                case ESCAPE -> {
                    if (mediaPlayer.fullScreen().isFullScreen()) {
                        mediaPlayer.fullScreen().toggle();
                    }
                }
                case F -> mediaPlayer.fullScreen().toggle();
                case Z -> videoImageView.setPreserveRatio(!videoImageView.isPreserveRatio());
            }
        });
        parentChildren.add(0, playerPane);
        parent.requestFocus();

        setLoading(true);
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

    public void play(String url) {
        setLoading(true);
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

    public void destroy() {
        mediaPlayer.release();
    }
}
