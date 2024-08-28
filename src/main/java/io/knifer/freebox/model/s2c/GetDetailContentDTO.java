package io.knifer.freebox.model.s2c;

import lombok.Data;

/**
 * 获取详情信息参数
 * @author knifer
 */
@Data
public class GetDetailContentDTO {

    /**
     * 源
     */
    private String sourceKey;

    /**
     * 视频ID
     */
    private String videoId;

    public static GetDetailContentDTO of(String sourceKey, String videoId) {
        GetDetailContentDTO result = new GetDetailContentDTO();

        result.setSourceKey(sourceKey);
        result.setVideoId(videoId);

        return result;
    }
}
