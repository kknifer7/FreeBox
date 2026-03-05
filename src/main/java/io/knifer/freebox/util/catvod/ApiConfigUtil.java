package io.knifer.freebox.util.catvod;

import cn.hutool.core.codec.Base64;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * 接口配置相关工具类
 *
 * @author Knifer
 */
@Slf4j
@UtilityClass
public class ApiConfigUtil {

    /**
     * 解析接口配置json文本
     * @param content 获取来的配置文本
     * @return json
     */
    public String parseApiConfigJson(String content) {
        String[] contentSplits;

        if (StringUtils.isBlank(content) || content.startsWith("{")) {

            return content;
        }
        // 有的站点（如饭太硬）返回的是“图片+Base64格式的json”，需要额外处理一下
        contentSplits = content.split("\\*\\*", 2);
        if (contentSplits.length != 2) {

            return content;
        }

        try {

            return Base64.decodeStr(contentSplits[1]);
        } catch (Exception e) {
            log.info("base64 decode failed", e);

            return StringUtils.EMPTY;
        }
    }
}
