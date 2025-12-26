package io.knifer.freebox.util.hls;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    /**
     * 根据m3u8文件行判断是否为主播放列表（包含子m3u8链接）
     * @param m3u8Lines m3u8文件行
     * @return bool
     */
    public boolean isMasterPlaylist(Collection<String> m3u8Lines) {
        return m3u8Lines.stream().anyMatch(line -> line.startsWith("#EXT-X-STREAM-INF:"));
    }

    /**
     * 从m3u8主播放列表中提取子m3u8URL，并根据baseUrl将遇到的相对URL转换为绝对URL
     * @param lines m3u8文件行
     * @param baseUrl m3u8文件所在目录/网址
     * @return 子m3u8URL列表
     */
    public List<String> getSubPlaylistUrls(List<String> lines, @Nullable String baseUrl) {
        List<String> urls = new ArrayList<>();
        boolean noBaseUrl = StringUtils.isBlank(baseUrl);
        String line;
        String urlLine;
        String fullUrl;

        for (int i = 0; i < lines.size(); i++) {
            line = lines.get(i);
            if (line.startsWith("#EXT-X-STREAM-INF:") && i + 1 < lines.size()) {
                urlLine = lines.get(i + 1);
                if (!urlLine.startsWith("#")) {
                    if (noBaseUrl || urlLine.startsWith("http://") || urlLine.startsWith("https://")) {
                        fullUrl = urlLine;
                    } else {
                        fullUrl = baseUrl + urlLine;
                    }
                    urls.add(fullUrl);
                }
            }
        }

        return urls;
    }
}
