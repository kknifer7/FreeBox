package io.knifer.freebox.helper;

import io.knifer.freebox.context.Context;
import lombok.experimental.UtilityClass;

/**
 * 主机服务
 *
 * @author Knifer
 */
@UtilityClass
public class HostServiceHelper {

    /**
     * 打开网页
     * @param url 网页地址
     */
    public void showDocument(String url) {
        Context.INSTANCE.getApp().getHostServices().showDocument(url);
    }
}
