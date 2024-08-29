package io.knifer.freebox.component.node;

import io.knifer.freebox.helper.WindowHelper;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
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
    private final Label pauseLabel;
    private final Slider volumeSlider;
    private final Label volumeLabel;
    private final ProgressBar videoProgressBar;
    private final Label videoProgressLabel;
    private final Label videoProgressSplitLabel;
    private final Label videoProgressLengthLabel;
    private final Label fullScreenLabel;
    private final Label fillWindowLabel;
    private final FontIcon pauseIcon = FontIcon.of(FontAwesome.PAUSE, 32, Color.WHITE);
    private final FontIcon playIcon = FontIcon.of(FontAwesome.PLAY, 32, Color.WHITE);
    private final FontIcon volumeUpIcon = FontIcon.of(FontAwesome.VOLUME_UP, 32, Color.WHITE);
    private final FontIcon volumeOffIcon = FontIcon.of(FontAwesome.VOLUME_OFF, 32, Color.WHITE);
    private final FontIcon fullScreenIcon = FontIcon.of(FontAwesome.ARROWS_ALT, 32, Color.WHITE);
    private final FontIcon fillWindowIcon = FontIcon.of(FontAwesome.WINDOW_MAXIMIZE, 32, Color.WHITE);
    private final AtomicLong videoLength = new AtomicLong(-1);
    private final AtomicBoolean isVideoProgressBarUsing = new AtomicBoolean(false);

    public VLCPlayer(BorderPane parent) {
        StackPane pane = new StackPane();
        ReadOnlyDoubleProperty parentWidthProp = parent.widthProperty();
        ReadOnlyDoubleProperty parentHeightProp = parent.heightProperty();
        Stage stage = WindowHelper.getStage(parent);
        List<Node> paneChildren;
        StackPane controlPane;
        AnchorPane controlInnerAnchorPane;
        PopOver volumePopOver;
        Timer volumePopOverHideTimer;
        HBox progressLabelHBox;
        HBox leftToolBarHbox;
        HBox rightToolBarHbox;

        stage.setFullScreenExitHint(StringUtils.EMPTY);
        stage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
        mediaPlayer = new MediaPlayerFactory().mediaPlayers().newEmbeddedMediaPlayer();
        mediaPlayer.fullScreen().strategy(new JavaFXFullScreenStrategy(stage){
            @Override
            public void onBeforeEnterFullScreen() {
                videoImageView.setPreserveRatio(false);
                /*parent.getTop().setVisible(false);
                parent.getLeft().setVisible(false);
                parent.getRight().setVisible(false);
                parent.getBottom().setVisible(false);*/
            }

            @Override
            public void onAfterExitFullScreen() {
                /*parent.getTop().setVisible(true);
                parent.getLeft().setVisible(true);
                parent.getRight().setVisible(true);
                parent.getBottom().setVisible(true);*/
            }
        });
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void playing(MediaPlayer mediaPlayer) {
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
        videoImageView.fitWidthProperty().bind(pane.widthProperty());
        videoImageView.fitHeightProperty().bind(pane.heightProperty());
        mediaPlayer.videoSurface().set(new ImageViewVideoSurface(videoImageView));
        pauseLabel = new Label();
        pauseLabel.setGraphic(pauseIcon);
        pauseLabel.getStyleClass().add("vlc-player-control-label");
        pauseLabel.setOnMouseClicked(evt -> changePlayStatus());
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
        volumePopOver.getStyleClass().add("vlc-player-volume-pop-over");
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
            mediaPlayer.controls().setPosition(((float) videoProgressBar.getProgress()));
            isVideoProgressBarUsing.set(false);
        });
        videoProgressSplitLabel = new Label("/");
        videoProgressSplitLabel.getStyleClass().add("vlc-player-progress-label");
        videoProgressLengthLabel = new Label("-:-:-");
        videoProgressLengthLabel.getStyleClass().add("vlc-player-progress-label");
        progressLabelHBox = new HBox(videoProgressLabel, videoProgressSplitLabel, videoProgressLengthLabel);
        progressLabelHBox.setSpacing(5);
        progressLabelHBox.setAlignment(Pos.CENTER);
        fullScreenLabel = new Label();
        fullScreenLabel.getStyleClass().add("vlc-player-control-label");
        fullScreenLabel.setGraphic(fullScreenIcon);
        fullScreenLabel.setOnMouseClicked(evt -> mediaPlayer.fullScreen().toggle());
        fillWindowLabel = new Label();
        fillWindowLabel.getStyleClass().add("vlc-player-control-label");
        fillWindowLabel.setGraphic(fillWindowIcon);
        fillWindowLabel.setOnMouseClicked(evt -> videoImageView.setPreserveRatio(!videoImageView.isPreserveRatio()));
        leftToolBarHbox = new HBox(pauseLabel, volumeLabel, progressLabelHBox);
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
        AnchorPane.setLeftAnchor(videoProgressBar, 360.0);
        AnchorPane.setRightAnchor(videoProgressBar, 160.0);
        AnchorPane.setTopAnchor(videoProgressBar, 10.0);
        AnchorPane.setBottomAnchor(videoProgressBar, 10.0);
        AnchorPane.setRightAnchor(rightToolBarHbox, 10.0);
        AnchorPane.setTopAnchor(rightToolBarHbox, 10.0);
        AnchorPane.setBottomAnchor(rightToolBarHbox, 10.0);
        controlPane = new StackPane(controlInnerAnchorPane);
        controlPane.setAlignment(Pos.BOTTOM_CENTER);
        paneChildren = pane.getChildren();
        paneChildren.add(videoImageView);
        paneChildren.add(controlPane);
        pane.prefWidthProperty().bind(parentWidthProp);
        pane.prefHeightProperty().bind(parentHeightProp);
        pane.minWidthProperty().bind(parentWidthProp);
        pane.minHeightProperty().bind(parentHeightProp);
        pane.maxWidthProperty().bind(parentWidthProp);
        pane.maxHeightProperty().bind(parentHeightProp);
        pane.setOnMouseClicked(evt -> {
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
        parent.requestFocus();
        parent.setCenter(pane);
    }

    public void play(String url) {
        long length;

        mediaPlayer.media().play(url);
        length = mediaPlayer.status().length();
        log.info("video length: {}", length);
        mediaPlayer.audio().setVolume((int) volumeSlider.getValue());
    }

    public void changePlayStatus() {
        mediaPlayer.controls().pause();
        pauseLabel.setGraphic(pauseLabel.getGraphic() == pauseIcon ? playIcon : pauseIcon);
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
