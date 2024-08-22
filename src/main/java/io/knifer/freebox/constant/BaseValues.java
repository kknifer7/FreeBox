package io.knifer.freebox.constant;

import lombok.experimental.UtilityClass;

/**
 * 常量
 *
 * @author Knifer
 */
@UtilityClass
public class BaseValues {

    /**
     * 默认HTTP服务端口
     */
    public static final Integer DEFAULT_HTTP_PORT = 9897;
    /**
     * 默认WS服务端口
     */
    public static final Integer DEFAULT_WS_PORT = 9898;
    public static final String REPOSITORY_URL = "https://github.com/kknifer7/FreeBox";
    public static final String REPOSITORY_NEW_ISSUE_URL = "https://github.com/kknifer7/FreeBox/issues/new";
    public static final String TV_BOX_K_REPOSITORY_URL = "https://github.com/kknifer7/TVBoxOS-K";
    public static final Long KEB_SOCKET_REQUEST_TIMEOUT = 12L;
}
