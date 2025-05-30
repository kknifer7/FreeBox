package io.knifer.freebox.model.common.tvbox;

import lombok.Data;

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

    public static VodCollect from(VodInfo vodInfo) {
        VodCollect vodCollect = new VodCollect();

        vodCollect.vodId = vodInfo.id;
        vodCollect.updateTime = System.currentTimeMillis();
        vodCollect.sourceKey = vodInfo.sourceKey;
        vodCollect.name = vodInfo.name;
        vodCollect.pic = vodInfo.pic;

        return vodCollect;
    }
}