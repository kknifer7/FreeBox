package io.knifer.freebox.model.s2c;

import lombok.Data;

import java.util.List;

/**
 * 获取播放信息
 * @author Knifer
 */
@Data
public class GetPlayerContentDTO {

    /**
     * 站点key
     */
    private String sourceKey;

    /**
     * 播放标志
     */
    private String playFlag;

    /**
     * 视频ID
     */
    private String vodId;

    /**
     * VIP解析标志
     */
    private List<String> vipParseFlags;

    public static GetPlayerContentDTO of(
            String sourceKey,
            String playFlag,
            String vodId
    ) {
        GetPlayerContentDTO result = new GetPlayerContentDTO();

        result.setSourceKey(sourceKey);
        result.setPlayFlag(playFlag);
        result.setVodId(vodId);

        return result;
    }
}
