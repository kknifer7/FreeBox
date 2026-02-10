package io.knifer.freebox.helper;

import io.knifer.freebox.context.Context;
import io.knifer.freebox.ioc.IOC;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;

/**
 * 主机服务
 *
 * @author Knifer
 */
@Slf4j
@UtilityClass
public class HostServiceHelper {

    /**
     * 打开网页
     * @param url 网页地址
     */
    public void showDocument(String url) {
        IOC.getBean(Context.class).getApp().getHostServices().showDocument(url);
    }

    /**
     * 在资源管理器中显示文件
     * @param file 文件
     * @return 是否成功
     */
    public boolean openFileInExplorer(File file) {
        if (file == null || !file.exists()) {
            log.warn("file not exists");

            return false;
        }
        switch (SystemHelper.getPlatform()) {
            case WINDOWS -> {
                try {
                    Runtime.getRuntime().exec("explorer /select,\"" + file.getPath() + "\"");
                } catch (IOException e) {
                    log.warn("windows open file in explorer failed", e);

                    return false;
                }
            }
            case MAC -> {
                try {
                    Runtime.getRuntime().exec(new String[] { "open", "-R", file.getPath() });
                } catch (IOException e) {
                    log.warn("macOS open file in explorer failed", e);

                    return false;
                }
            }
            case DEB_LINUX, RPM_LINUX, OTHER_LINUX -> {
                try {
                    Runtime.getRuntime().exec("xdg-open \"" + file.getParent() + "\"");
                } catch (IOException e) {
                    log.warn("linux open file in explorer failed", e);

                    return false;
                }
            }
        }

        return true;
    }
}
