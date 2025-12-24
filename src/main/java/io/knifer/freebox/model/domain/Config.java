package io.knifer.freebox.model.domain;

import io.knifer.freebox.constant.PlayerType;
import io.knifer.freebox.constant.VideoPlaybackTrigger;
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
     * 应用版本
     */
    private String appVersion;

    /**
     * 应用版本码
     */
    private Integer appVersionCode;

    /**
     * 是否展示许可协议
     */
    private Boolean showLicense;

    /**
     * 自动检查更新
     */
    private Boolean autoCheckUpgrade;

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

    /**
     * 字体
     */
    private String usageFontFamily;

    /**
     * 影视播放触发方式
     */
    private VideoPlaybackTrigger videoPlaybackTrigger;

    /**
     * 启用广告过滤
     */
    private Boolean adFilter;

    /**
     * 正态分布强制广告过滤 - 动态阈值因子
     * 越低越会采取激进的策略识别广告
     * -1时禁用强制过滤
     */
    private Double adFilterDynamicThresholdFactor;

    /**
     * 播放器类型
     */
    private PlayerType playerType;

    /**
     * mpv播放器路径
     */
    private String mpvPath;

    /**
     * vlc播放器路径（vlcj NativeDiscovery失败时使用）
     */
    private String vlcPath;
}
