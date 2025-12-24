package io.knifer.freebox.util;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;

/**
 * Url相关工具类
 *
 * @author Knifer
 */
@UtilityClass
public class UrlUtil {

    @Nullable
    public String getParent(String url) {
        int lastSlashIdx = url.lastIndexOf('/');

        return lastSlashIdx != -1 ? url.substring(0, lastSlashIdx + 1) : null;
    }

    public String resolveRelative(String relativeUrl, String baseUrl) {
        if (
                StringUtils.isBlank(baseUrl) ||
                relativeUrl.startsWith("http://") ||
                relativeUrl.startsWith("https://")
        ) {

            return relativeUrl;
        }
        baseUrl = StringUtils.appendIfMissing(baseUrl, "/");
        baseUrl = StringUtils.removeStart(baseUrl, '/');

        return baseUrl + relativeUrl;
    }
}
