package io.knifer.freebox.model.s2c;

import io.knifer.freebox.model.common.tvbox.VodInfo;
import lombok.Data;

/**
 * 收藏影片
 * @author knifer
 */
@Data
public class SaveMovieCollectionDTO {

    /**
     * 影片信息
     */
    private VodInfo vodInfo;

    public static SaveMovieCollectionDTO of(VodInfo vodInfo) {
        SaveMovieCollectionDTO result = new SaveMovieCollectionDTO();

        result.setVodInfo(vodInfo);

        return result;
    }
}
