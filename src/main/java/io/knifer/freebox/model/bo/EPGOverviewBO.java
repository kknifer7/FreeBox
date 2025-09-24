package io.knifer.freebox.model.bo;

import io.knifer.freebox.model.domain.LiveChannel;
import io.knifer.freebox.model.domain.LiveChannelGroup;
import lombok.Data;

import javax.annotation.Nullable;
import java.util.List;

/**
 * EPG浏览数据
 *
 * @author Knifer
 */
@Data
public class EPGOverviewBO {

    /**
     * 直播频道分组（所有数据）
     */
    private List<LiveChannelGroup> liveChannelGroups;

    /**
     * 默认的直播频道
     */
    private LiveChannel defaultLiveChannel;

    /**
     * EPG服务地址
     */
    private String epgServiceUrl;

    public static EPGOverviewBO of(
            List<LiveChannelGroup> liveChannelGroups,
            LiveChannel defaultLiveChannel,
            @Nullable String epgServiceUrl
    ) {
        EPGOverviewBO epgOverviewBO = new EPGOverviewBO();

        epgOverviewBO.setLiveChannelGroups(liveChannelGroups);
        epgOverviewBO.setDefaultLiveChannel(defaultLiveChannel);
        epgOverviewBO.setEpgServiceUrl(epgServiceUrl);

        return epgOverviewBO;
    }
}
