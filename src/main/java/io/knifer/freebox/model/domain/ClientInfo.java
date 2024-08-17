package io.knifer.freebox.model.domain;

import com.google.common.base.Strings;
import io.knifer.freebox.model.c2s.RegisterInfo;
import lombok.Data;
import org.java_websocket.WebSocket;

/**
 * 客户端信息
 *
 * @author Knifer
 */
@Data
public class ClientInfo {

    private String name;

    private WebSocket connection;

    public static ClientInfo of(RegisterInfo registerInfo, WebSocket connection) {
        ClientInfo clientInfo = new ClientInfo();

        clientInfo.setName(Strings.nullToEmpty(registerInfo.getName()));
        clientInfo.setConnection(connection);

        return clientInfo;
    }
}
