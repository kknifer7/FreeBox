package io.knifer.freebox.constant;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.SystemUtils;

/**
 * 常量
 *
 * @author Knifer
 */
@UtilityClass
public class BaseValues {

    /**
     * 任意本地IP
     */
    public static final String ANY_LOCAL_IP = "0.0.0.0";
    /**
     * 默认HTTP服务端口
     */
    public static final Integer DEFAULT_HTTP_PORT = 9978;
    /**
     * 默认WS服务端口
     */
    public static final Integer DEFAULT_WS_PORT = 9898;
    /**
     * 首页链接相关
     */
    public static final String REPOSITORY_URL = "https://github.com/kknifer7/FreeBox";
    public static final String REPOSITORY_NEW_ISSUE_URL = "https://github.com/kknifer7/FreeBox/issues/new";
    public static final String TV_BOX_K_REPOSITORY_URL = "https://github.com/kknifer7/TVBoxOS-K";
    public static final String VLC_DOWNLOAD_URL = "https://mirror.nju.edu.cn/videolan-ftp/vlc/3.0.21/win32/vlc-3.0.21-win32.exe";
    /**
     * KebSocket通信超时时间
     */
    public static final Long KEB_SOCKET_REQUEST_TIMEOUT = 12L;
    /**
     * “获取更多”项ID
     */
    public static final String LOAD_MORE_ITEM_ID = "load-more";

    /**
     * x.properties key
     */
    public static final String X_APP_VERSION = "app-version";
    public static final String X_APP_VERSION_CODE = "app-version-code";
    public static final String X_UPGRADE_CONFIG_URL = "upgrade-config-url";
    public static final String X_DEBUG = "debug";

    /**
     * FXML相关默认值
     */
    public static final double DEFAULT_WINDOW_WIDTH = 960;
    public static final double DEFAULT_WINDOW_HEIGHT = 720;
    public static final double DEFAULT_DIALOG_WIDTH = 600;
    public static final double DEFAULT_DIALOG_HEIGHT = 400;

    /**
     * 其他
     */
    public static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
    public static final Runnable EMPTY_RUNNABLE = () -> {};
    public static final String HTTP_GET = "GET";
    public static final Platform CURRENT_PLATFORM;

    static {
        if (SystemUtils.IS_OS_WINDOWS) {
            CURRENT_PLATFORM = Platform.WINDOWS;
        } else if (SystemUtils.IS_OS_MAC) {
            CURRENT_PLATFORM = Platform.MAC;
        } else if (SystemUtils.IS_OS_LINUX) {
            CURRENT_PLATFORM = Platform.LINUX;
        } else {
            throw new AssertionError();
        }
    }
}
