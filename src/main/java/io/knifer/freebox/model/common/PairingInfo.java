package io.knifer.freebox.model.common;

import lombok.Data;

/**
 * 配对信息
 *
 * @author Knifer
 */
@Data
public class PairingInfo {

    private String address;

    private Integer port;

    public static PairingInfo from(String address, Integer port) {
        PairingInfo info = new PairingInfo();

        info.setAddress(address);
        info.setPort(port);

        return info;
    }
}
