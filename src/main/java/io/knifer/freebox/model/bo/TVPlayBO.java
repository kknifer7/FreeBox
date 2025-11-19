package io.knifer.freebox.model.bo;

import lombok.Data;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * 点播影片播放参数
 *
 * @author Knifer
 */
@Data
public class TVPlayBO {

    /**
     * 播放地址
     */
    private String url;

    /**
     * 请求头
     */
    private Map<String, String> headers;

    /**
     * 影片标题
     */
    private String videoTitle;

    /**
     * 播放进度
     */
    @Nullable
    private Long progress;

    /**
     * 广告过滤是否生效
     */
    private boolean adFiltered;

    public static TVPlayBO of(
            String url,
            Map<String, String> headers,
            String videoTitle,
            @Nullable Long progress,
            boolean adFiltered
    ) {
        TVPlayBO result = new TVPlayBO();

        result.url = url;
        result.headers = headers;
        result.videoTitle = videoTitle;
        result.progress = progress;
        result.adFiltered = adFiltered;

        return result;
    }
}
