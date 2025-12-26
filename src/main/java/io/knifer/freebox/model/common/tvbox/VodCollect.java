package io.knifer.freebox.model.common.tvbox;

import io.knifer.freebox.model.common.catvod.Keep;
import io.knifer.freebox.model.s2c.SaveMovieCollectionDTO;
import lombok.Data;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * 收藏影片
 *
 * @author Cuke
 */
@Data
public class VodCollect implements Serializable {
    private int id;
    public String vodId;
    public long updateTime;
    public String sourceKey;
    public String name;
    public String pic;

    public static VodCollect from(SaveMovieCollectionDTO dto) {
        VodCollect vodCollect = new VodCollect();

        vodCollect.vodId = dto.getVodId();
        vodCollect.updateTime = System.currentTimeMillis();
        vodCollect.sourceKey = dto.getSourceKey();
        vodCollect.name = dto.getVodName();
        vodCollect.pic = dto.getVodPic();

        return vodCollect;
    }

    public static VodCollect from(Keep keep) {
        VodCollect vodCollect = new VodCollect();
        String key = keep.getKey();
        String[] keySplit = StringUtils.split(key, "@@@");

        vodCollect.vodId = ArrayUtils.get(keySplit, 1);
        vodCollect.updateTime = keep.getCreateTime();
        vodCollect.sourceKey = ArrayUtils.get(keySplit, 0);
        vodCollect.name = keep.getVodName();
        vodCollect.pic = keep.getVodPic();

        return vodCollect;
    }
}