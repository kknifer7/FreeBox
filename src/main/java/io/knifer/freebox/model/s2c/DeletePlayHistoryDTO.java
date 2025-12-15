package io.knifer.freebox.model.s2c;

import io.knifer.freebox.model.common.tvbox.VodInfo;
import lombok.Data;

/**
 * 删除观看历史
 *
 * @author knifer
 */
@Data
public class DeletePlayHistoryDTO {

    /**
     * 站点ID
     */
    private String sourceKey;

    /**
     * 影片ID
     */
    private String vodId;

    public static DeletePlayHistoryDTO of(VodInfo vodInfo) {
        DeletePlayHistoryDTO result = new DeletePlayHistoryDTO();

        result.setSourceKey(vodInfo.getSourceKey());
        result.setVodId(vodInfo.getId());

        return result;
    }
}
