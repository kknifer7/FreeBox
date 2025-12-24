package io.knifer.freebox.model.s2c;

import io.knifer.freebox.model.bo.VideoPlayInfoBO;
import io.knifer.freebox.model.common.tvbox.Movie;
import io.knifer.freebox.model.common.tvbox.VodInfo;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 保存观看历史
 * @author knifer
 */
@Data
public class SavePlayHistoryDTO {

    /**
     * 站点key
     */
    private String sourceKey;

    /**
     * 影片ID
     */
    private String vodId;

    /**
     * 影片名称
     */
    private String vodName;

    /**
     * 影片海报
     */
    private String vodPic;

    /**
     * 播放源名称
     */
    private String playFlag;

    /**
     * 播放的ep名称
     */
    private String episodeFlag;

    /**
     * 播放的ep地址
     */
    private String episodeUrl;

    /**
     * 播放的ep索引
     */
    private int episodeIndex;

    /**
     * 是否倒序
     */
    private boolean revSort;

    /**
     * 播放位置
     */
    private long position;

    /**
     * 影片时长
     */
    private long duration;

    public static SavePlayHistoryDTO of(
            VodInfo vodInfo, Movie.Video.UrlBean.UrlInfo.InfoBean episode, VideoPlayInfoBO playInfo
    ) {
        SavePlayHistoryDTO result = new SavePlayHistoryDTO();

        result.setSourceKey(vodInfo.getSourceKey());
        result.setVodId(vodInfo.getId());
        result.setVodName(vodInfo.getName());
        result.setVodPic(vodInfo.getPic());
        result.setEpisodeFlag(episode.getName());
        result.setPlayFlag(playInfo.getPlayFlag());
        result.setEpisodeIndex(vodInfo.getPlayIndex());
        result.setEpisodeUrl(episode.getUrl());
        result.setRevSort(vodInfo.isReverseSort());
        result.setPosition(ObjectUtils.defaultIfNull(vodInfo.getProgress(), 0L));
        result.setDuration(ObjectUtils.defaultIfNull(playInfo.getDuration(), 0L));

        return result;
    }
}
