package io.knifer.freebox.model.s2c;

import io.knifer.freebox.model.common.tvbox.VodInfo;
import lombok.Data;

/**
 * 保存观看历史
 * @author knifer
 */
@Data
public class SavePlayHistoryDTO {

    /**
     * 影片信息
     */
    private VodInfo vodInfo;

    public static SavePlayHistoryDTO of(VodInfo vodInfo) {
        SavePlayHistoryDTO result = new SavePlayHistoryDTO();

        result.setVodInfo(vodInfo);

        return result;
    }
}
