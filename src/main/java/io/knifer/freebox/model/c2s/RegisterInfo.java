package io.knifer.freebox.model.c2s;

import io.knifer.freebox.constant.KType;
import lombok.Data;

/**
 * 注册信息
 *
 * @author Knifer
 */
@Data
public class RegisterInfo {

    /**
     * 远程客户端ID
     */
    private String clientId;

    /**
     * 远程客户端名称
     */
    private String clientName;

    /**
     * 远程客户端类型
     */
    private KType kType;

    /**
     * 协议版本号
     */
    private Integer protocolVersionCode;
}
