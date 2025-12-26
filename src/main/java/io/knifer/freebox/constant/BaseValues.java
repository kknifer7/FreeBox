package io.knifer.freebox.constant;

import com.google.common.net.HttpHeaders;
import lombok.experimental.UtilityClass;

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
    public static final String REPOSITORY_NEW_ISSUE_URL =
            "https://github.com/kknifer7/FreeBox/issues/new?template=%E6%8A%A5%E5%91%8A%E6%A8%A1%E6%9D%BF.md";
    public static final String TV_BOX_K_REPOSITORY_URL = "https://github.com/kknifer7/TV-K";
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
    public static final String X_SUPPORTED_KEB_SOCKET_PROTOCOL_VERSION_CODE =
            "supported-keb-socket-protocol-version-code";
    public static final String X_UPGRADE_CONFIG_URL = "upgrade-config-url";
    public static final String X_DEBUG = "debug";

    /**
     * FXML相关默认值
     */
    public static final double DEFAULT_WINDOW_WIDTH = 960;
    public static final double DEFAULT_WINDOW_HEIGHT = 720;
    public static final double DEFAULT_DIALOG_WIDTH = 600;
    public static final double DEFAULT_DIALOG_HEIGHT = 500;

    /**
     * 其他
     */
    public static final String SUCCESS_STR = "1";
    public static final String FAIL_STR = "0";
    public static final String USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
    public static final Runnable EMPTY_RUNNABLE = () -> {};
    public static final String HTTP_GET = "GET";
    public static final String[] FETCH_CAT_VOD_API_CONFIG_HTTP_HEADERS = new String[]{
            HttpHeaders.USER_AGENT, "okhttp/3.15",
            HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"
    };
}
