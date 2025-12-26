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
     * 影片ID
     */
    private String vodId;
    /**
     * 站点key
     */
    private String sourceKey;
    /**
     * 影片名称
     */
    private String vodName;
    /**
     * 影片海报
     */
    private String vodPic;

    public static SaveMovieCollectionDTO of(VodInfo vodInfo) {
        SaveMovieCollectionDTO result = new SaveMovieCollectionDTO();

        result.setVodId(vodInfo.getId());
        result.setSourceKey(vodInfo.getSourceKey());
        result.setVodName(vodInfo.getName());
        result.setVodPic(vodInfo.getPic());

        return result;
    }
}
