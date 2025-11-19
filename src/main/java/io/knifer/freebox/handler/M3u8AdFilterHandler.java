package io.knifer.freebox.handler;

import io.knifer.freebox.model.domain.M3u8AdFilterResult;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * m3u8切片广告过滤
 */
public interface M3u8AdFilterHandler {

    /**
     * 过滤切片广告
     * @param url m3u8源地址（用于拼接绝对路径）
     * @param content m3u8文件内容
     * @param extraData 扩展数据
     * @return 过滤后的m3u8文件内容（如果文件含有多个子播放列表，则返回合并后的结果；如果读取m3u8失败，返回null）
     */
    M3u8AdFilterResult handle(
            String url, String content, @Nullable Map<String, Object> extraData
    ) throws Exception;
}
