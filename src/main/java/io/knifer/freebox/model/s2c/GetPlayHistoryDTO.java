package io.knifer.freebox.model.s2c;

import lombok.Data;

/**
 * 获取观看历史
 * @author knifer
 */
@Data
public class GetPlayHistoryDTO {

    /**
     * 数据条数
     */
    private Integer limit;

    public static GetPlayHistoryDTO of(Integer limit) {
        GetPlayHistoryDTO result = new GetPlayHistoryDTO();

        result.setLimit(limit);

        return result;
    }
}
