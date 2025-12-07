package io.knifer.freebox.component.node.player;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.macfja.mpv.Service;
import io.github.macfja.mpv.communication.handling.NamedEventHandler;
import io.github.macfja.mpv.communication.handling.PropertyObserver;
import io.github.macfja.mpv.wrapper.Shorthand;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.ConfigHelper;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.service.FutureWaitingService;
import io.knifer.freebox.util.AsyncUtil;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 外部MPV播放器
 *
 * @author Knifer
 */
@Slf4j
public class MPVExternalPlayer extends BasePlayer<StackPane> {

    private Shorthand mpv;
    private CompletableFuture<Void> playerInitialized;
    private final AtomicLong playingResourceId = new AtomicLong();
    private final Lock playingResourceLock = new ReentrantLock();
    private final ExecutorService playbackExecutor = Executors.newSingleThreadExecutor();
    private ScheduledFuture<?> catchProgressTask;
    private final ScheduledExecutorService catchProgressExecutor;
    private final AtomicLong progressCaught;
    private final AtomicLong resumeProgress;
    private final AtomicInteger ipcFailedCount;

    private final static List<String> MPV_EXTRA_ARGS = List.of("--idle=yes", "--force-window=no");

    public MPVExternalPlayer(Pane parent, Config config) {
        super(parent, config);
        catchProgressExecutor = config.getLiveMode() ? null : Executors.newSingleThreadScheduledExecutor();
        progressCaught = new AtomicLong(0);
        resumeProgress = new AtomicLong(0);
        ipcFailedCount = new AtomicInteger(0);
        initPlayer();
    }

    private void initPlayer() {
        if (destroyFlag) {
            log.info("player destroyed, cancel init new player");

            return;
        }
        showToast(I18nHelper.get(I18nKeys.VIDEO_EXTERNAL_PLAYER_LOADING));
        playerInitialized = new CompletableFuture<>();
        mpv = new Shorthand(new Service(ConfigHelper.getMpvPath(), MPV_EXTRA_ARGS));
        AsyncUtil.execute(() -> {
            mpv.waitForEvent("idle");
            Platform.runLater(this::registerEventAndPropertyChange);
            playerInitialized.complete(null);
        });
    }

    private void registerEventAndPropertyChange() {
        try {
            mpv.registerEvent(new NamedEventHandler("end-file") {
                @Override
                public Runnable doHandle(JsonObject params) {
                    JsonElement reasonElm;
                    String reason;

                    if (config.getLiveMode()) {

                        return null;
                    }
                    if (catchProgressTask != null && !catchProgressTask.isDone() && !catchProgressTask.isCancelled()) {
                        catchProgressTask.cancel(true);
                    }
                    reasonElm = params.get("reason");
                    if (reasonElm == null) {
                        progressCaught.set(0);
                        log.info("mpv end-file, reason=null");

                        return null;
                    }
                    reason = reasonElm.getAsString();
                    log.info("mpv end-file, reason={}", reason);
                    if ("eof".equals(reason)) {
                        progressCaught.set(0);
                        postFinished();
                    }

                    return null;
                }
            });
            mpv.registerPropertyChange(new PropertyObserver("seekable") {
                @Override
                public void changed(String propertyName, Object value, Integer id) {
                    long progress = resumeProgress.get();

                    if (progress > 0 && value instanceof Boolean seekable && seekable) {
                        try {
                            mpv.seek((int) (progress / 1000), Shorthand.Seek.Absolute);
                        } catch (IOException e) {
                            log.error("mpv ipc error (seek)", e);
                            Platform.runLater(() -> showToast(I18nHelper.getFormatted(
                                    I18nKeys.VIDEO_EXTERNAL_PLAYER_IPC_FAILED, e.getMessage()
                            )));
                        }
                        resumeProgress.set(0);
                    }
                }
            });
        } catch (IOException e) {
            log.error("mpv register property change error", e);
        }
    }

    @Override
    protected StackPane createPlayerNode() {
        Label label = new Label(I18nHelper.get(I18nKeys.VIDEO_USING_EXTERNAL_PLAYER));
        StackPane centerStackPane = new StackPane(label);

        label.getStyleClass().add("loading-label");

        return centerStackPane;
    }

    @Override
    protected boolean doPlay(String url, Map<String, String> headers, String videoTitle, @Nullable Long progress) {
        FutureWaitingService<Void> futureWaitingService;
        long playingResourceId;

        if (destroyFlag) {
            log.info("player destroyed, cancel play");

            return false;
        }
        doPlayInit(progress);
        if (!super.doPlay(url, headers, videoTitle, progress) || !playerInitialized.isDone()) {
            futureWaitingService = new FutureWaitingService<>(playerInitialized);
            futureWaitingService.setOnSucceeded(ignored -> doPlay(url, headers, videoTitle, progress));
            futureWaitingService.start();
        }
        playingResourceId = IdUtil.getSnowflakeNextId();
        this.playingResourceId.set(playingResourceId);
        log.info("play url={}", url);
        playbackExecutor.execute(() -> {
            boolean successFlag = false;

            try {
                playingResourceLock.lock();
                if (playingResourceId != this.playingResourceId.get()) {

                    return;
                }
                mpv.addMedia(url, false);
                mpv.setProperty("title", videoTitle);
                mpv.waitForEvent("playback-restart");
                if (!config.getLiveMode()) {
                    catchProgressTask = catchProgressExecutor.scheduleWithFixedDelay(
                            () -> {
                                Integer val;

                                try {
                                    val = mpv.getProperty("time-pos", Integer.class);
                                } catch (Exception e) {
                                    log.warn("cancel catchProgressTask, because mpv get time-pos error", e);
                                    if (catchProgressTask != null) {
                                        catchProgressTask.cancel(true);
                                    }

                                    return;
                                }
                                if (val != null && val > 0) {
                                    val = val * 1000;
                                    progressCaught.set(val);
                                    log.debug("mpv progress caught: {}", val);
                                }
                            },
                            2,
                            5,
                            TimeUnit.SECONDS
                    );
                }
                successFlag = true;
                ipcFailedCount.set(0);
            } catch (IOException e) {
                log.error("mpv ipc error", e);
                Platform.runLater(() -> showToast(I18nHelper.getFormatted(
                        I18nKeys.VIDEO_EXTERNAL_PLAYER_IPC_FAILED, e.getMessage()
                )));
            } catch (Exception e) {
                log.error("mpv unknown error", e);
                Platform.runLater(() -> ToastHelper.showException(e));
            } finally {
                playingResourceLock.unlock();
            }
            if (!successFlag) {
                IoUtil.close(mpv);
                if (!config.getLiveMode() && catchProgressTask != null) {
                    catchProgressTask.cancel(true);
                }
                if (ipcFailedCount.incrementAndGet() >= 3) {
                    Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.VIDEO_EXTERNAL_PLAYER_UNAVAILABLE));
                    destroy();

                    return;
                }
                Platform.runLater(() -> {
                    initPlayer();
                    doPlay(url, headers, videoTitle, progress);
                });
            }
        });

        return true;
    }

    private void doPlayInit(@Nullable Long progress) {
        progressCaught.set(0);
        if (progress != null) {
            resumeProgress.set(progress);
        }
        if (
                !config.getLiveMode() &&
                        catchProgressTask != null &&
                        !catchProgressTask.isDone() &&
                        !catchProgressTask.isCancelled()
        ) {
            catchProgressTask.cancel(true);
        }
    }

    @Override
    public void destroy() {
        if (destroyFlag) {

            return;
        }
        super.destroy();
        if (!config.getLiveMode()) {
            catchProgressExecutor.shutdownNow();
        }
        try {
            mpv.close();
        } catch (IOException e) {
            log.warn("mpv close error", e);
        }
        log.info("destroy mpv external player");
    }

    @Override
    public long getCurrentProgress() {
        Integer progress;

        try {
            progress = mpv.getProperty("time-pos", Integer.class);

            return progress == null || progress < 0 ? 0 : progress * 1000;
        } catch (IOException e) {
            log.warn("mpv get time-pos error", e);

            return progressCaught.get();
        }
    }
}
