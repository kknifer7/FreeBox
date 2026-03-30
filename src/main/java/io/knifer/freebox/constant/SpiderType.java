package io.knifer.freebox.constant;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.io.File;

/**
 * 爬虫类型
 *
 * @author knifer
 */
public enum SpiderType {

    JS;

    @Nullable
    public static SpiderType from(File spiderFile) {
        String path = spiderFile.getAbsolutePath();

        if (StringUtils.endsWithIgnoreCase(path, ".js")) {

            return JS;
        }

        return null;
    }
}
