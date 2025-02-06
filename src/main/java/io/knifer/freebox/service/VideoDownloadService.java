package io.knifer.freebox.service;

import io.knifer.freebox.util.hls.HLSUtil;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;

/**
 * 视频下载服务
 *
 * @author Knifer
 */
@Slf4j
@RequiredArgsConstructor
public class VideoDownloadService extends Service<Void> {

    private final String videoUrl;
    private final String outputFile;

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                if (StringUtils.contains(videoUrl, ".m3u8")) {
                    // TODO 实现HLS下载
                    // downloadHLS();
                } else {
                    downloadSingleVideo();
                }

                return null;
            }

            private void downloadHLS() {
                try {
                    HLSUtil.downloadVideo(videoUrl, outputFile);
                } catch (IOException e) {
                    log.error("download hls video failed, url={}, outputFile={}", videoUrl, outputFile, e);
                    setException(e);
                }
            }

            private void downloadSingleVideo() {

            }
        };
    }
}
