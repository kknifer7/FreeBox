package io.knifer.freebox.model.s2c;

import io.knifer.freebox.model.common.tvbox.VodInfo;
import lombok.Data;

/**
 * 取消收藏
 *
 * @author knifer
 */
@Data
public class DeleteMovieCollectionDTO {

    /**
     * 影片信息
     */
    private VodInfo vodInfo;

    public static DeleteMovieCollectionDTO of(VodInfo vodInfo) {
        DeleteMovieCollectionDTO result = new DeleteMovieCollectionDTO();

        result.setVodInfo(vodInfo);

        return result;
    }
}
