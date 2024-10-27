package io.knifer.freebox.model.s2c;

import lombok.Data;

/**
 * 获取一个观看历史
 * @author knifer
 */
@Data
public class GetOnePlayHistoryDTO {

    /**
     * 数据条数
     */
    private String sourceKey;

    /**
     * 影片ID
     */
    private String vodId;

    public static GetOnePlayHistoryDTO of(String sourceKey, String vodId) {
        GetOnePlayHistoryDTO result = new GetOnePlayHistoryDTO();

        result.setSourceKey(sourceKey);
        result.setVodId(vodId);

        return result;
    }
}
