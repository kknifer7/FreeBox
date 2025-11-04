package io.knifer.freebox.service;

import cn.hutool.core.io.StreamProgress;
import cn.hutool.http.HttpUtil;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.util.Pair;
import lombok.RequiredArgsConstructor;

import java.io.File;
import java.util.function.Consumer;

/**
 * 下载服务
 *
 * @author Knifer
 */
@RequiredArgsConstructor
public class DownloadService extends Service<Void> {

    private final String url;
    private final File savePath;
    private final Runnable onStart;
    private final Consumer<Pair<Long, Long>> onProgress;
    private final Runnable onFinish;

    private long lastProgressSize = 0;

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                HttpUtil.downloadFileFromUrl(
                        url,
                        savePath,
                        new StreamProgress() {
                            @Override
                            public void start() {
                                if (isCancelled()) {
                                    return;
                                }
                                if (onStart != null) {
                                    onStart.run();
                                }
                            }

                            @Override
                            public void progress(long total, long progressSize) {
                                if (isCancelled() || progressSize - lastProgressSize < 1024) {
                                    lastProgressSize = progressSize;

                                    return;
                                }
                                lastProgressSize = progressSize;
                                if (onProgress != null) {
                                    onProgress.accept(new Pair<>(total, progressSize));
                                }
                            }

                            @Override
                            public void finish() {
                                if (isCancelled()) {
                                    return;
                                }
                                if (onFinish != null) {
                                    onFinish.run();
                                }
                            }
                        }
                );

                return null;
            }
        };
    }
}
