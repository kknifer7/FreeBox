package io.knifer.freebox.handler.impl;

import cn.hutool.core.text.StrPool;
import com.google.common.net.HttpHeaders;
import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.handler.M3u8TsProxyHandler;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.UrlUtil;
import io.knifer.freebox.util.hls.HLSUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 非标准m3u8 ts代理处理器
 * 将后缀名不是"ts"的链接通过本地代理服务进行代理
 *
 * @author Knifer
 */
@Slf4j
public class BadM3u8TsProxyHandler implements M3u8TsProxyHandler {

    @Override
    public Pair<Boolean, String> handle(String m3u8Url, String content, String proxyUrlPrefix) {
        List<String> lines = List.of(content.split("\n"));
        String baseUrl = UrlUtil.getParent(m3u8Url);
        Pair<Boolean, String> result;

        if (HLSUtil.isMasterPlaylist(lines)) {
            result = processMasterPlaylist(lines, baseUrl, proxyUrlPrefix);
            if (!result.getLeft()) {
                result.setValue(content);
            }
        } else {
            result = processSinglePlaylist(lines, baseUrl, proxyUrlPrefix);
        }
        log.info("fixed bad m3u8: {}", result.getLeft());

        return result;
    }

    /**
     * 处理主播放列表
     */
    private Pair<Boolean, String> processMasterPlaylist(List<String> lines, String baseUrl, String proxyUrlPrefix) {
        List<String> subPlaylistUrls = HLSUtil.getSubPlaylistUrls(lines, baseUrl);
        List<String> processedSubPlaylistContents = new ArrayList<>(subPlaylistUrls.size());
        boolean hasModified = false;
        String subPlaylistFullUrl;
        String subPlaylistContent;
        Pair<Boolean, String> result;

        for (String subPlaylistUrl : subPlaylistUrls) {
            try {
                subPlaylistFullUrl = UrlUtil.resolveRelative(subPlaylistUrl, baseUrl);
                subPlaylistContent = HttpUtil.getAsync(
                        subPlaylistFullUrl,
                        HttpHeaders.USER_AGENT,
                        BaseValues.USER_AGENT
                ).get(6, TimeUnit.SECONDS);
                if (StringUtils.isBlank(subPlaylistContent)) {
                    log.warn("subPlaylist is empty, url={}", subPlaylistUrl);
                    continue;
                }
                result = processSinglePlaylist(
                        List.of(subPlaylistContent.split("\n")),
                        UrlUtil.getParent(subPlaylistFullUrl),
                        proxyUrlPrefix
                );
                if (result.getLeft() && !hasModified) {
                    hasModified = true;
                }
                processedSubPlaylistContents.add(result.getRight());
            } catch (Exception e) {
                log.error("process subPlaylist failed, url={}", subPlaylistUrl, e);
            }
        }
        return hasModified ?
                Pair.of(true, mergeSubPlaylists(processedSubPlaylistContents)) :
                MutablePair.of(false, null);
    }

    /**
     * 处理单个播放列表
     */
    private Pair<Boolean, String> processSinglePlaylist(List<String> lines, String baseUrl, String proxyUrlPrefix) {
        List<String> processedLines = new ArrayList<>(lines.size());
        boolean hasModified = false;
        String processedLine;

        for (String line : lines) {
            processedLine = processLine(line, baseUrl, proxyUrlPrefix);
            if (!processedLine.equals(line) && !hasModified) {
                hasModified = true;
            }
            processedLines.add(processedLine);
        }

        return Pair.of(hasModified, StringUtils.join(processedLines, StrPool.LF));
    }

    /**
     * 处理单行内容
     */
    private String processLine(String line, String baseUrl, String proxyUrlPrefix) {
        String url;

        if (!isUrlLine(line)) {

            return line;
        }
        url = UrlUtil.resolveRelative(line, baseUrl);

        return url.toLowerCase().contains(".ts") ? url : proxyUrlPrefix + url;
    }

    /**
     * 判断是否为URL行
     */
    private boolean isUrlLine(String line) {
        return StringUtils.isNotBlank(line) &&
                !line.startsWith("#") &&
                (line.contains("://") || line.contains("/"));
    }

    /**
     * 合并子播放列表
     */
    private String mergeSubPlaylists(List<String> subPlaylistContents) {
        List<String> mergedLines;
        boolean isFirstPlaylist;
        List<String> lines;
        boolean inHeader;

        if (subPlaylistContents.isEmpty()) {

            return StringUtils.EMPTY;
        }
        mergedLines = new ArrayList<>();
        isFirstPlaylist = true;
        for (String content : subPlaylistContents) {
            if (StringUtils.isBlank(content)) {
                continue;
            }
            lines = List.of(content.split("\n"));
            inHeader = true;
            for (String line : lines) {
                if (inHeader && line.startsWith("#EXTINF:")) {
                    inHeader = false;
                }
                if (inHeader) {
                    if (isFirstPlaylist) {
                        mergedLines.add(line);
                    }
                    continue;
                }
                mergedLines.add(line);
            }
            isFirstPlaylist = false;
        }

        return StringUtils.join(mergedLines, StrPool.LF);
    }
}