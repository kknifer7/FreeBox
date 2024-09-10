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
     * 源
     */
    private String sourceKey;

    /**
     * 进度
     */
    private String processKey;

    /**
     * 播放标志
     */
    private String playFlag;

    /**
     * 视频ID
     */
    private String id;

    /**
     * VIP解析标志
     */
    private List<String> vipParseFlags;

    public static GetPlayerContentDTO of(
            String sourceKey,
            String processKey,
            String playFlag,
            String id
    ) {
        GetPlayerContentDTO result = new GetPlayerContentDTO();

        result.setSourceKey(sourceKey);
        result.setProcessKey(processKey);
        result.setPlayFlag(playFlag);
        result.setId(id);

        return result;
    }
}
