package io.knifer.freebox.model.s2c;

import lombok.Data;

/**
 * 获取影视收藏状态
 * @author knifer
 */
@Data
public class GetMovieCollectedStatusDTO {

    /**
     * 数据条数
     */
    private String sourceKey;

    /**
     * 影片ID
     */
    private String vodId;

    public static GetMovieCollectedStatusDTO of(String sourceKey, String vodId) {
        GetMovieCollectedStatusDTO result = new GetMovieCollectedStatusDTO();

        result.setSourceKey(sourceKey);
        result.setVodId(vodId);

        return result;
    }
}
