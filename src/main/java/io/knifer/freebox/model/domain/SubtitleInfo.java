package io.knifer.freebox.model.domain;

import lombok.Data;

/**
 * 字幕信息
 *
 * @author Knifer
 */
@Data
public class SubtitleInfo {

    private String name;

    private String url;

    /**
     * 是否为压缩文件
     */
    private boolean archiveFlag;

    public static SubtitleInfo of(String name, String url, boolean archiveFlag) {
        SubtitleInfo subtitleInfo = new SubtitleInfo();

        subtitleInfo.setName(name);
        subtitleInfo.setUrl(url);
        subtitleInfo.setArchiveFlag(archiveFlag);

        return subtitleInfo;
    }
}
