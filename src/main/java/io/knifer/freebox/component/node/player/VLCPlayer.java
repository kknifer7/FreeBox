package io.knifer.freebox.component.node.player;

import cn.hutool.core.util.IdUtil;
import io.knifer.freebox.helper.SystemHelper;
import io.knifer.freebox.helper.WindowHelper;
import io.knifer.freebox.util.AsyncUtil;
import javafx.application.Platform;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.fullscreen.JavaFXFullScreenStrategy;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.State;
import uk.co.caprica.vlcj.player.base.SubpictureApi;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import javax.annotation.Nullable;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * VLC播放器自定义组件
 *
 * @author Knifer
 */
@Slf4j
public class VLCPlayer extends BasePlayer<ImageView> {

    private int trackId = -1;

    private final EmbeddedMediaPlayer mediaPlayer;

    private final AtomicLong playingResourceId = new AtomicLong();
    private final Lock playingResourceLock = new ReentrantLock();
    private final ExecutorService playbackExecutor = Executors.newSingleThreadExecutor();

    public VLCPlayer(Pane parent, Config config) {
        super(parent, config);

        if (BooleanUtils.isNotFalse(config.getExternalMode())) {
            config.setExternalMode(false);
        }

        Stage stage = WindowHelper.getStage(parent);
        MediaPlayerFactory mediaPlayerFactory;

        mediaPlayerFactory = SystemHelper.isDebug() ?
                new MediaPlayerFactory(List.of("-vvv")) : new MediaPlayerFactory();
        mediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
        mediaPlayer.fullScreen().strategy(new JavaFXFullScreenStrategy(stage){
            @Override
            public void onBeforeEnterFullScreen() {
                Platform.runLater(() -> postEnterFullScreen());
            }

            @Override
            public void onAfterExitFullScreen() {
                Platform.runLater(() -> postExitFullScreen());
            }
        });
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void mediaPlayerReady(MediaPlayer mediaPlayer) {
                Platform.runLater(() -> setLoading(false));
            }

            @Override
            public void buffering(MediaPlayer mediaPlayer, float newCache) {
                Platform.runLater(() -> postBuffering(newCache));
            }

            @Override
            public void paused(MediaPlayer mediaPlayer) {
                Platform.runLater(() -> postPaused());
            }

            @Override
            public void playing(MediaPlayer mediaPlayer) {
                Platform.runLater(() -> postPlaying());
            }

            @Override
            @SuppressWarnings("ConstantConditions")
            public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
                Platform.runLater(() -> postLengthChanged(newLength));
            }

            @Override
            @SuppressWarnings("ConstantConditions")
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
               Platform.runLater(() ->  postTimeChanged(newTime));
            }

            @Override
            public void finished(MediaPlayer mediaPlayer) {
                postFinished();
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                Platform.runLater(() -> postError());
            }
        });
        mediaPlayer.videoSurface().set(new ImageViewVideoSurface(playerNode));
    }

    @Override
    protected ImageView createPlayerNode() {
        ImageView videoImageView = new ImageView();

        videoImageView.setPreserveRatio(true);
        videoImageView.fitWidthProperty().bind(playerPane.widthProperty());
        videoImageView.fitHeightProperty().bind(playerPane.heightProperty());

        return videoImageView;
    }

    @Override
    protected void movePosition(boolean forward) {
        long length = mediaPlayer.status().length();
        long oldTime;
        long newTime;

        if (!mediaPlayer.status().isPlayable() || length <= 0) {
            return;
        }
        oldTime = mediaPlayer.status().time();
        newTime = forward ? Math.min(oldTime + 5000, length) : Math.max(oldTime - 5000, 0);
        mediaPlayer.controls().setTime(newTime);
        postMovedPosition();
    }

    @Override
    protected void moveVolume(boolean forward) {
        AsyncUtil.execute(() -> {
            double oldVolume = mediaPlayer.audio().volume();
            double newVolume = forward ? Math.min(oldVolume + 10, 100) : Math.max(oldVolume - 10, 0);

            if (mediaPlayer.audio().isMute()) {
                mediaPlayer.audio().mute();
                Platform.runLater(() -> {
                    postMuted();
                    postMovedVolume(newVolume);
                });
            } else {
                Platform.runLater(() -> postMovedVolume(newVolume));
            }
        });
    }

    @Override
    protected boolean doPlay(String url, Map<String, String> headers, String videoTitle, @Nullable Long progress) {
        String[] options;
        long playingResourceId;

        if (!super.doPlay(url, headers, videoTitle, progress)) {

            return false;
        }
        options = parsePlayOptionsFromHeaders(headers);
        // 为防止频繁切换播放资源时多次调用play，导致播放器长时间阻塞，这里为资源生成唯一ID，利用单线程池+锁的方式将播放任务串行化
        playingResourceId = IdUtil.getSnowflakeNextId();
        this.playingResourceId.set(playingResourceId);
        log.info("play url={}, options={}", url, options);
        playbackExecutor.execute(() -> {
            try {
                playingResourceLock.lock();
                if (playingResourceId != this.playingResourceId.get()) {

                    return;
                }
            } finally {
                playingResourceLock.unlock();
            }
            mediaPlayer.media().play(url, options);
        });

        return true;
    }

    @Nullable
    private String[] parsePlayOptionsFromHeaders(Map<String, String> headers) {
        String userAgent;
        String referer;
        short size;

        if (headers.isEmpty()) {
            return new String[] {
                    "--subsdec-encoding=UTF-8"
            };
        }
        userAgent = null;
        referer = null;
        size = 0;
        for (Map.Entry<String, String> keyValue : headers.entrySet()) {
            if (StringUtils.equalsIgnoreCase(keyValue.getKey(), "User-Agent")) {
                size++;
                userAgent = "--http-user-agent=" + keyValue.getValue();
            } else if (StringUtils.equalsIgnoreCase(keyValue.getKey(), "Referer")) {
                size++;
                referer = "--http-referer=" + keyValue.getValue();
            }
        }
        switch (size) {
            case 0:
                return null;
            case 1:
                if (userAgent != null) {
                    return new String[]{"--subsdec-encoding=UTF-8", userAgent};
                } else {
                    return new String[]{"--subsdec-encoding=UTF-8", referer};
                }
            default:
                return new String[]{"--subsdec-encoding=UTF-8", userAgent, referer};
        }
    }

    @Override
    public void togglePause() {
        if (isError() || isLoading()) {

            return;
        }
        AsyncUtil.execute(() -> {
            boolean canPause = mediaPlayer.status().canPause();

            if (!canPause) {

                return;
            }
            // 调用暂停API时可能出现短暂延迟，为用户体验考虑，显示一下loading告知用户等待
            Platform.runLater(() -> setLoading(true));
            mediaPlayer.controls().pause();
        });
    }

    public void stop() {
        setLoading(false);
        AsyncUtil.execute(() -> {
            State playerState = mediaPlayer.status().state();

            if (playerState != State.STOPPED && playerState != State.ENDED) {
                mediaPlayer.controls().stop();
            }
        });
    }

    @Override
    public long getCurrentProgress() {
        long length = mediaPlayer.status().length();
        float position;

        if (length < 1) {
            return 0;
        }
        position = mediaPlayer.status().position();

        return position == 0 ? 0 : ((long) (position * length));
    }

    @Override
    public void destroy() {
        super.destroy();
        AsyncUtil.execute(() -> {
            if (mediaPlayer.status().isPlaying()) {
                SystemHelper.allowSleep();
            }
            playbackExecutor.shutdownNow();
            mediaPlayer.release();
            log.info("vlc media player released");
        });
    }

    @Override
    protected void setPlayTime(long time) {
        AsyncUtil.execute(() -> mediaPlayer.controls().setTime(time));

    }

    @Override
    protected void setVolume(int volume) {
        AsyncUtil.execute(() -> mediaPlayer.audio().setVolume(volume));
    }

    @Override
    protected void setSubtitleDelay(long delay) {
        AsyncUtil.execute(() -> mediaPlayer.subpictures().setDelay(delay * 1000));
    }

    @Override
    protected boolean useSubtitle(File subtitleFile) {
        if (mediaPlayer.status().isPlayable()) {

            return mediaPlayer.subpictures().setSubTitleFile(subtitleFile);
        }

        return false;
    }

    @Override
    protected void setSubtitleVisible(boolean visible) {
        SubpictureApi mediaPlayerSubpictureApi = mediaPlayer.subpictures();

        AsyncUtil.execute(() -> {
            if (visible) {
                if (mediaPlayerSubpictureApi.track() == -1) {
                    if (trackId != -1) {
                        mediaPlayerSubpictureApi.setTrack(trackId);
                    } else {
                        mediaPlayerSubpictureApi.trackDescriptions()
                                .stream()
                                .filter(td -> td.id() != -1)
                                .findFirst()
                                .ifPresent(td -> {
                                    trackId = td.id();
                                    mediaPlayerSubpictureApi.setTrack(trackId);
                                });
                    }
                }
            } else if ((trackId = mediaPlayerSubpictureApi.track()) != -1) {
                mediaPlayerSubpictureApi.setTrack(-1);
            }
        });
    }

    @Override
    protected void toggleMute() {
        AsyncUtil.execute(() -> mediaPlayer.audio().mute());
    }

    @Override
    protected boolean isMute() {
        return mediaPlayer.audio().isMute();
    }

    @Override
    protected void setRate(float rate) {
        mediaPlayer.controls().setRate(rate);
    }

    @Override
    protected void setFillWindow(boolean fillWindow) {
        playerNode.setPreserveRatio(!fillWindow);
    }

    @Override
    protected void play() {
        AsyncUtil.execute(() -> mediaPlayer.controls().play());
    }

    @Override
    protected void toggleFullScreen() {
        mediaPlayer.fullScreen().toggle();
    }

    @Override
    protected boolean isFullScreen() {
        return mediaPlayer.fullScreen().isFullScreen();
    }

    @Override
    protected boolean isSeekable() {
        return mediaPlayer.status().isPlayable() && mediaPlayer.status().isSeekable();
    }

    @Override
    protected void setPositionPercent(float positionPercent) {
        mediaPlayer.controls().setPosition(positionPercent);
    }
}
