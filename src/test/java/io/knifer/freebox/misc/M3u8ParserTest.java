package io.knifer.freebox.misc;

import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.helper.I18nHelper;
import io.knifer.freebox.model.domain.LiveChannel;
import io.knifer.freebox.model.domain.LiveChannelGroup;
import net.bjoernpetersen.m3u.M3uParser;
import net.bjoernpetersen.m3u.model.M3uEntry;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Knifer
 */
public class M3u8ParserTest {

    @Test
    void test() throws IOException {
//        List<M3uEntry> entries = M3uParser.parse(HttpUtil.get("https://ghfast.top/https://raw.githubusercontent.com/YueChan/Live/refs/heads/main/APTV.m3u"));
        List<M3uEntry> m3uEntries = M3uParser.parse(Path.of("E:\\下载\\APTV.m3u"));
        Map<String, LiveChannelGroup> groupTitleAndLiveChannelGroupMap;
        Map<String, LiveChannel> titleAndLiveChannelMap;
        AtomicInteger unTitledChannelIdx;
        List<LiveChannelGroup> groups;

        if (m3uEntries.isEmpty()) {

            return;
        }
        groupTitleAndLiveChannelGroupMap = new HashMap<>();
        titleAndLiveChannelMap = new HashMap<>();
        unTitledChannelIdx = new AtomicInteger(0);
        groups = m3uEntries.stream().map(m3uEntry -> {
            String groupTitle;
            LiveChannelGroup group;
            LiveChannel channel;
            String title;
            List<LiveChannel> groupChannels;

            groupTitle = m3uEntry.getMetadata().get("group-title");
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
            channel = titleAndLiveChannelMap.get(title);
            if (channel == null) {
                channel = LiveChannel.of(groupTitle, title);
                titleAndLiveChannelMap.put(title, channel);
                groupChannels.add(channel);
            }
            channel.getLines().add(LiveChannel.Line.of(m3uEntry.getTitle(), null, m3uEntry.getLocation().toString()));

            return group;
        }).toList();
        System.out.println(groupTitleAndLiveChannelGroupMap);
    }
}
