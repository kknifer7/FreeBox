package io.knifer.freebox.model.domain;

import lombok.Data;

/**
 * m3u8广告过滤结果
 *
 * @author Knifer
 */
@Data
public class M3u8AdFilterResult {

    /**
     * 过滤的广告行数（建议只统计被过滤的ts行数）
     */
    private int adLineCount;

    /**
     * m3u8内容
     */
    private String content;

    public static M3u8AdFilterResult of(int adLineCount, String content) {
        M3u8AdFilterResult result = new M3u8AdFilterResult();

        result.content = content;
        result.adLineCount = adLineCount;

        return result;
    }
}
