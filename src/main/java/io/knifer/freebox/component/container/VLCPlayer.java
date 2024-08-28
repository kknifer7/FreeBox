package io.knifer.freebox.component.container;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

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
    private final FontIcon pauseIcon = FontIcon.of(FontAwesome.PAUSE, 32, Color.WHITE);
    private final FontIcon playIcon = FontIcon.of(FontAwesome.PLAY, 32, Color.WHITE);

    public VLCPlayer(BorderPane parent) {
        StackPane pane = new StackPane();
        ReadOnlyDoubleProperty parentWidthProp = parent.widthProperty();
        ReadOnlyDoubleProperty parentHeightProp = parent.heightProperty();
        List<Node> paneChildren;
        StackPane toolBarPane;
        ToolBar toolBar;

        mediaPlayer = new MediaPlayerFactory().mediaPlayers().newEmbeddedMediaPlayer();
        /*mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void playing(MediaPlayer mediaPlayer) {
                log.info("playing");
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                log.info("error");
            }
        });*/
        videoImageView = new ImageView();
        videoImageView.setPreserveRatio(true);
        videoImageView.fitWidthProperty().bind(pane.widthProperty());
        videoImageView.fitHeightProperty().bind(pane.heightProperty());
        mediaPlayer.videoSurface().set(new ImageViewVideoSurface(videoImageView));
        pauseLabel = new Label();
        pauseLabel.setGraphic(pauseIcon);
        pauseLabel.setOnMouseClicked(evt -> changePlayStatus());
        toolBar = new ToolBar(pauseLabel);
        toolBar.getStyleClass().add("vlc-player-tool-bar");
        toolBarPane = new StackPane(toolBar);
        toolBarPane.setAlignment(Pos.BOTTOM_CENTER);
        paneChildren = pane.getChildren();
        paneChildren.add(videoImageView);
        paneChildren.add(toolBarPane);
        pane.prefWidthProperty().bind(parentWidthProp);
        pane.prefHeightProperty().bind(parentHeightProp);
        pane.minWidthProperty().bind(parentWidthProp);
        pane.minHeightProperty().bind(parentHeightProp);
        pane.maxWidthProperty().bind(parentWidthProp);
        pane.maxHeightProperty().bind(parentHeightProp);
        parent.setCenter(pane);
    }

    public void play(String url) {
        mediaPlayer.media().play(url);
    }

    public void changePlayStatus() {
        mediaPlayer.controls().pause();
        pauseLabel.setGraphic(pauseLabel.getGraphic() == pauseIcon ? playIcon : pauseIcon);
    }
}
