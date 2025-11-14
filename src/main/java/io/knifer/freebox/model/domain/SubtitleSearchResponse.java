package io.knifer.freebox.model.domain;

import lombok.Data;

import java.util.List;

/**
 * 字幕信息响应
 *
 * @author Knifer
 */
@Data
public class SubtitleSearchResponse {

    private Integer page;

    private Integer totalPage;

    private List<SubtitleInfo> subtitleInfoList;

    private static final SubtitleSearchResponse EMPTY;

    static {
        EMPTY = new SubtitleSearchResponse();
        EMPTY.setPage(0);
        EMPTY.setTotalPage(0);
        EMPTY.setSubtitleInfoList(List.of());
    }

    public static SubtitleSearchResponse empty() {
        return EMPTY;
    }

    public static SubtitleSearchResponse of(Integer page, Integer totalPage, List<SubtitleInfo> subtitleInfoList) {
        SubtitleSearchResponse subtitleSearchResponse = new SubtitleSearchResponse();

        subtitleSearchResponse.setPage(page);
        subtitleSearchResponse.setTotalPage(totalPage);
        subtitleSearchResponse.setSubtitleInfoList(subtitleInfoList);

        return subtitleSearchResponse;
    }
}
