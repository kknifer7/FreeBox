package io.knifer.freebox.model.common.catvod;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/**
 * 历史记录
 * 该类和该包下大部分的类都来源于FongMi影视，因其中的部分字段命名易与TVBox的混淆，特此记录：
 *     key: 历史记录/收藏记录的主键，格式为“站点key@@@影片ID@@@cid”
 *     vodFlag: 播放源的名称
 *     vodRemarks: 播放剧集的名称（如“第一集”、“第二集”）而非影视备注
 * 有坑注意：在FongMi影视中，影片只有单个线路（源）时，vodFlag会被赋值为vodRemarks，多线路时又会被赋值为播放源的名称，
 *     由此导致的歧义让vodFlag字段变得很不可靠，因此，不要完全依赖vodFlag来判断播放源
 */
@Data
public class History {

    @SerializedName("key")
    private String key;
    @SerializedName("vodPic")
    private String vodPic;
    @SerializedName("vodName")
    private String vodName;
    @SerializedName("vodFlag")
    private String vodFlag;
    @SerializedName("vodRemarks")
    private String vodRemarks;
    @SerializedName("episodeUrl")
    private String episodeUrl;
    @SerializedName("revSort")
    private boolean revSort;
    @SerializedName("revPlay")
    private boolean revPlay;
    @SerializedName("createTime")
    private long createTime;
    @SerializedName("opening")
    private long opening;
    @SerializedName("ending")
    private long ending;
    @SerializedName("position")
    private long position;
    @SerializedName("duration")
    private long duration;
    @SerializedName("speed")
    private float speed;
    @SerializedName("scale")
    private int scale;
    @SerializedName("cid")
    private int cid;
}
