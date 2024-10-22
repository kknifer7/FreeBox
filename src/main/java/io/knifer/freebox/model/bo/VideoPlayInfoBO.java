package io.knifer.freebox.model.bo;

import io.knifer.freebox.model.common.VodInfo;
import lombok.Data;

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

    public static VideoPlayInfoBO of(VodInfo vodInfo) {
        VideoPlayInfoBO result = new VideoPlayInfoBO();

        result.setSourceKey(vodInfo.getSourceKey());
        result.setPlayFlag(vodInfo.getPlayFlag());
        result.setPlayIndex(vodInfo.getPlayIndex());
        result.setReverseSort(vodInfo.isReverseSort());
        result.setProgress(vodInfo.getProgress());

        return result;
    }
}
