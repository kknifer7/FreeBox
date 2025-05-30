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
     * 影片信息
     */
    private VodInfo vodInfo;

    public static DeletePlayHistoryDTO of(VodInfo vodInfo) {
        DeletePlayHistoryDTO result = new DeletePlayHistoryDTO();

        result.setVodInfo(vodInfo);

        return result;
    }
}
