package io.knifer.freebox.util.hls;

import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * hls工具类
 * 相关代码来源：<a href="https://github.com/nichucs/HLS2MP4">...</a>
 *
 * @author Knifer
 */
@UtilityClass
public class HLSUtil {

    public void downloadVideo(String url, String outfile) throws IOException {
        createHlsDownloader(url).download(outfile);
    }

    public PlaylistDownloader createHlsDownloader(String url) throws MalformedURLException {
        return new PlaylistDownloader(url);
    }
}
