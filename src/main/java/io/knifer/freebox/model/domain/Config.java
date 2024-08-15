package io.knifer.freebox.model.domain;

import lombok.Data;

/**
 * 本地设置
 *
 * @author Knifer
 */
@Data
public class Config {

    /**
     * 唯一标识
     */
    private String uuid;

    /**
     * HTTP服务端口
     */
    private Integer httpPort;

    /**
     * WebSocket服务端口
     */
    private Integer wsPort;

    /**
     * 服务IPv4地址
     */
    private String serviceIPv4;

    /**
     * 自动启动HTTP服务
     */
    private Boolean autoStartHttp;

    /**
     * 自动启动WebSocket服务
     */
    private Boolean autoStartWs;
}
