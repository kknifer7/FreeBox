package io.knifer.freebox.model.bo;

import io.knifer.freebox.model.common.tvbox.VodInfo;
import lombok.Data;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 影片播放信息
 *
 * @author Knifer
 */
@Data
public class VideoPlayInfoBO {

    /**
     * 源key
     */
    private String sourceKey;

    /**
     * 播放源名称
     */
    private String playFlag;

    /**
     * 播放列表下标
     */
    private int playIndex;

    /**
     * 是否倒序播放列表
     */
    private boolean reverseSort;

    /**
     * 播放进度
     */
    private Long progress;

    /**
     * 播放集数名称（续播历史记录时，若无playIndex，则使用该属性匹配出playIndex）
     */
    private String playNote;

    /**
     * 影片时长
     */
    private Long duration;

    public static VideoPlayInfoBO of(VodInfo vodInfo) {
        VideoPlayInfoBO result = new VideoPlayInfoBO();

        result.setSourceKey(vodInfo.getSourceKey());
        result.setPlayFlag(vodInfo.getPlayFlag());
        result.setPlayIndex(vodInfo.getPlayIndex());
        result.setReverseSort(vodInfo.isReverseSort());
        result.setProgress(ObjectUtils.defaultIfNull(vodInfo.getProgress(), 0L));
        result.setDuration(ObjectUtils.defaultIfNull(vodInfo.getDuration(), 0L));
        result.setPlayNote(vodInfo.getPlayNote());

        return result;
    }
}
