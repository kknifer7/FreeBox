package io.knifer.freebox.model.domain;

import io.knifer.freebox.constant.BaseValues;
import io.knifer.freebox.util.NetworkUtil;
import lombok.Data;

import java.util.UUID;

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
     * 服务网卡
     */
    private String networkInterface;

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

    public static Config createDefault() {
        // TODO 创建默认配置
        Config config = new Config();

        config.setUuid(UUID.randomUUID().toString());
        config.setHttpPort(BaseValues.DEFAULT_HTTP_PORT);
        config.setWsPort(BaseValues.DEFAULT_WS_PORT);
        NetworkUtil.getAvailableNetworkInterfaceAndIPv4();

        return config;
    }
}
