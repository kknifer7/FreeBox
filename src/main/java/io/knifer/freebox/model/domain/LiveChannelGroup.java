package io.knifer.freebox.model.domain;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 直播频道组
 *
 * @author Knifer
 */
@Data
public class LiveChannelGroup {

    /**
     * 频道组标题（作为频道的分组依据）
     */
    private String title;

    /**
     * 频道列表
     */
    private List<LiveChannel> channels;

    public static LiveChannelGroup from(String title) {
        LiveChannelGroup result = new LiveChannelGroup();

        result.setTitle(title);
        result.setChannels(new ArrayList<>());

        return result;
    }
}
