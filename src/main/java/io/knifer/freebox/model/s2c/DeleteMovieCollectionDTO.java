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
     * 站点key
     */
    private String sourceKey;

    /**
     * 影片ID
     */
    private String vodId;

    public static DeleteMovieCollectionDTO of(VodInfo vodInfo) {
        DeleteMovieCollectionDTO result = new DeleteMovieCollectionDTO();

        result.setSourceKey(vodInfo.getSourceKey());
        result.setVodId(vodInfo.getId());

        return result;
    }
}
