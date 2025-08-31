package io.knifer.freebox.model.domain;

import lombok.Data;

/**
 * 从 m3u/m3u8 中读取的待合并的直播频道
 *
 * @author Knifer
 */
@Data
public class LiveM3u8 {

    /**
     * 频道组标题（作为频道的分组依据）
     */
    private String groupTitle;

    /**
     * 频道标题（作为区分频道多线路的依据）
     */
    private String title;

    /**
     * 频道地址
     */
    private String url;
}
