package io.knifer.freebox.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.map.multi.RowKeyTable;
import cn.hutool.core.map.multi.Table;
import cn.hutool.crypto.digest.DigestUtil;
import com.google.common.base.Charsets;
import com.google.common.net.HttpHeaders;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.c2s.FreeBoxLive;
import io.knifer.freebox.model.domain.LiveChannel;
import io.knifer.freebox.model.domain.LiveChannelGroup;
import io.knifer.freebox.util.HttpUtil;
import io.knifer.freebox.util.ValidationUtil;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bjoernpetersen.m3u.M3uParser;
import net.bjoernpetersen.m3u.model.M3uEntry;
import net.bjoernpetersen.m3u.model.M3uMetadata;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 加载直播频道分组服务
 *
 * @author Knifer
 */
@Slf4j
@AllArgsConstructor
public class LoadLiveChannelGroupService extends Service<List<LiveChannelGroup>> {

    private final FreeBoxLive live;
    private final Path liveConfigCachePath;

    @Override
    protected Task<List<LiveChannelGroup>> createTask() {
        return new Task<>() {
            @Override
            protected List<LiveChannelGroup> call() {
                String url = live.getUrl();
                String liveMd5;
                Path livePath;
                String ua;
                String filePath;
                String liveConfigContent;
                List<LiveChannelGroup> liveChannelGroups;

                if (!ValidationUtil.isURL(url)) {
                    Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.LIVE_MESSAGE_INVALID_LIVE_URL));

                    return List.of();
                }
                if (url.contains("127.0.0.1")) {
                    Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.VIDEO_ERROR_SOURCE_NOT_SUPPORTED));

                    return List.of();
                }
                liveMd5 = DigestUtil.md5Hex(url);
                livePath = liveConfigCachePath.resolve(liveMd5);
                if (Files.exists(livePath)) {
                    liveConfigContent = FileUtil.readString(livePath.toFile(), Charsets.UTF_8);
                } else {
                    ua = live.getUa();
                    if (url.startsWith("file:///")) {
                        filePath = url.substring(7);
                        liveConfigContent = FileUtil.readString(filePath, Charsets.UTF_8);
                    } else {
                        try {
                            liveConfigContent = StringUtils.isBlank(ua) ?
                                    HttpUtil.getAsync(url).get(10, TimeUnit.SECONDS) :
                                    HttpUtil.getAsync(url, HttpHeaders.USER_AGENT, ua).get(10, TimeUnit.SECONDS);
                        } catch (TimeoutException | ExecutionException e) {
                            if (!isCancelled()) {
                                Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.LIVE_MESSAGE_INVALID_LIVE));
                            }

                            return List.of();
                        } catch (InterruptedException e) {
                            if (!isCancelled()) {
                                Platform.runLater(() -> ToastHelper.showException(e));
                            }

                            return List.of();
                        }
                    }
                    if (isCancelled()) {

                        return List.of();
                    }
                    if (StringUtils.isBlank(liveConfigContent)) {
                        Platform.runLater(() -> ToastHelper.showErrorI18n(I18nKeys.LIVE_MESSAGE_INVALID_LIVE));

                        return List.of();
                    }
                    liveConfigContent = liveConfigContent.trim();
                    FileUtil.writeString(liveConfigContent, livePath.toFile(), Charsets.UTF_8);
                }
                if (url.contains(".m3u") || liveConfigContent.startsWith("#")) {
                    liveChannelGroups = parseM3uLive(liveConfigContent);
                } else {
                    liveChannelGroups = parseGenreLive(liveConfigContent);
                }

                return ObjectUtils.defaultIfNull(liveChannelGroups, List.of());
            }

            private List<LiveChannelGroup> parseM3uLive(String liveConfigContent) {
                List<M3uEntry> m3uEntries = M3uParser.parse(liveConfigContent);
                Map<String, LiveChannelGroup> groupTitleAndLiveChannelGroupMap;
                Table<String, String, LiveChannel> groupTitleAndTitleAndLiveChannelMap;
                AtomicInteger unTitledChannelIdx;

                if (m3uEntries.isEmpty()) {

                    return List.of();
                }
                groupTitleAndLiveChannelGroupMap = new HashMap<>();
                groupTitleAndTitleAndLiveChannelMap = new RowKeyTable<>();
                unTitledChannelIdx = new AtomicInteger(0);
                m3uEntries.forEach(m3uEntry -> {
                    M3uMetadata metadata;
                    String groupTitle;
                    LiveChannelGroup group;
                    LiveChannel channel;
                    String title;
                    List<LiveChannel> groupChannels;
                    String logoUrl;
                    List<LiveChannel.Line> lines;

                    metadata = m3uEntry.getMetadata();
                    groupTitle = metadata.get("group-title");
                    if (StringUtils.isBlank(groupTitle)) {
                        groupTitle = I18nHelper.get(I18nKeys.LIVE_UNGROUPED);
                    }
                    group = groupTitleAndLiveChannelGroupMap.get(groupTitle);
                    if (group == null) {
                        group = LiveChannelGroup.from(groupTitle);
                        groupTitleAndLiveChannelGroupMap.put(groupTitle, group);
                    }
                    groupChannels = group.getChannels();
                    title = m3uEntry.getTitle();
                    if (StringUtils.isBlank(title)) {
                        title = I18nHelper.get(I18nKeys.LIVE_UNTITLED) + unTitledChannelIdx.getAndIncrement();
                    }
                    channel = groupTitleAndTitleAndLiveChannelMap.get(groupTitle, title);
                    if (channel == null) {
                        channel = LiveChannel.of(groupTitle, title);
                        groupTitleAndTitleAndLiveChannelMap.put(groupTitle, title, channel);
                        groupChannels.add(channel);
                    }
                    logoUrl = metadata.get("tvg-logo");
                    lines = channel.getLines();
                    if (StringUtils.isBlank(logoUrl)) {
                        // 如果logoUrl为空，则尝试使用相同频道的不同线路的logoUrl
                        logoUrl = lines.stream()
                                .map(LiveChannel.Line::getLogoUrl)
                                .filter(StringUtils::isNotBlank)
                                .findFirst()
                                .orElse(null);
                    }
                    channel.getLines().add(LiveChannel.Line.of(
                            I18nHelper.get(I18nKeys.LIVE_LINE) + (lines.size() + 1),
                            logoUrl,
                            m3uEntry.getLocation().toString()
                    ));
                });

                return new ArrayList<>(groupTitleAndLiveChannelGroupMap.values());
            }

            private List<LiveChannelGroup> parseGenreLive(String liveConfigContent) {
                String[] txtLines = liveConfigContent.split("\\r?\\n");
                String[] txtLineSplit;
                Map<String, LiveChannelGroup> groupTitleAndLiveChannelGroupMap;
                Map<String, LiveChannel> titleAndLiveChannelMap;
                String groupTitle = I18nHelper.get(I18nKeys.LIVE_UNGROUPED);
                LiveChannelGroup group = null;
                String channelTitle;
                LiveChannel channel;
                List<LiveChannelGroup> groups;
                List<LiveChannel.Line> channelLines;
                String[] channelLineTitleSplit;
                String channelLineTitle;

                if (txtLines.length < 2) {

                    return List.of();
                }
                groupTitleAndLiveChannelGroupMap = new HashMap<>();
                titleAndLiveChannelMap = new HashMap<>();
                groups = new ArrayList<>();
                for (String line : txtLines) {
                    txtLineSplit = line.split(",");
                    if (txtLineSplit.length != 2) {

                        continue;
                    }
                    if (txtLineSplit[1].strip().equals("#genre#")) {
                        // 频道分组
                        groupTitle = txtLineSplit[0];
                        group = groupTitleAndLiveChannelGroupMap.get(groupTitle);
                        if (group == null) {
                            group = LiveChannelGroup.from(groupTitle);
                            groupTitleAndLiveChannelGroupMap.put(groupTitle, group);
                            groups.add(group);
                        }
                    } else {
                        // 频道
                        if (group == null) {
                            // 频道没有分组信息，创建一个默认的“未分组”频道分组
                            group = LiveChannelGroup.from(groupTitle);
                            groupTitleAndLiveChannelGroupMap.put(groupTitle, group);
                            groups.add(group);
                        }
                        channelTitle = txtLineSplit[0];
                        channel = titleAndLiveChannelMap.get(channelTitle);
                        if (channel == null) {
                            channel = LiveChannel.of(groupTitle, channelTitle);
                            titleAndLiveChannelMap.put(channelTitle, channel);
                            group.getChannels().add(channel);
                        }
                        channelLines = channel.getLines();
                        // 解析线路名称
                        channelLineTitleSplit = txtLineSplit[1].split("『");
                        if (channelLineTitleSplit.length == 2) {
                            channelLineTitle = channelLineTitleSplit[1]
                                    .substring(0, channelLineTitleSplit[1].length() - 1);
                        } else {
                            channelLineTitle = I18nHelper.get(I18nKeys.LIVE_LINE) + (channelLines.size() + 1);
                        }
                        channelLines.add(LiveChannel.Line.of(channelLineTitle, null, txtLineSplit[1]));
                    }
                }

                return groups;
            }
        };
    }
}
