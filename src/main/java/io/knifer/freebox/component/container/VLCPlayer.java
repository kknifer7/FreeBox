package io.knifer.freebox.component.container;

import io.knifer.freebox.helper.WindowHelper;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.event.Event;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
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

/**
 * VLC播放器自定义组件
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
    private final Label fullScreenLabel;
    private final FontIcon pauseIcon = FontIcon.of(FontAwesome.PAUSE, 32, Color.WHITE);
    private final FontIcon playIcon = FontIcon.of(FontAwesome.PLAY, 32, Color.WHITE);
    private final FontIcon volumeUpIcon = FontIcon.of(FontAwesome.VOLUME_UP, 32, Color.WHITE);
    private final FontIcon volumeOffIcon = FontIcon.of(FontAwesome.VOLUME_OFF, 32, Color.WHITE);
    private final FontIcon fullScreenIcon = FontIcon.of(FontAwesome.ARROWS_ALT, 32, Color.WHITE);

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
                videoImageView.setPreserveRatio(true);
                /*parent.getTop().setVisible(true);
                parent.getLeft().setVisible(true);
                parent.getRight().setVisible(true);
                parent.getBottom().setVisible(true);*/
            }
        });
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void playing(MediaPlayer mediaPlayer) {
            }

            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                log.info("error");
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
        fullScreenLabel = new Label();
        fullScreenLabel.getStyleClass().add("vlc-player-control-label");
        fullScreenLabel.setGraphic(fullScreenIcon);
        fullScreenLabel.setOnMouseClicked(evt -> mediaPlayer.fullScreen().toggle());
        leftToolBarHbox = new HBox(pauseLabel, volumeLabel);
        leftToolBarHbox.setSpacing(20);
        leftToolBarHbox.setAlignment(Pos.CENTER);
        rightToolBarHbox = new HBox(fullScreenLabel);
        rightToolBarHbox.setSpacing(20);
        controlInnerAnchorPane = new AnchorPane(leftToolBarHbox, rightToolBarHbox);
        controlInnerAnchorPane.getStyleClass().add("vlc-player-anchor-pane");
        controlInnerAnchorPane.setOnMouseClicked(Event::consume);
        AnchorPane.setLeftAnchor(leftToolBarHbox, 10.0);
        AnchorPane.setRightAnchor(rightToolBarHbox, 10.0);
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

    public void destroy() {
        mediaPlayer.release();
    }
}
