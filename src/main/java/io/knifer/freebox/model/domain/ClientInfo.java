package io.knifer.freebox.model.domain;

import com.google.common.base.Strings;
import io.knifer.freebox.model.c2s.RegisterInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.java_websocket.WebSocket;

/**
 * 客户端信息
 *
 * @author Knifer
 */
@Getter
@EqualsAndHashCode
@ToString
public class ClientInfo {

    private String clientId;

    private String clientName;

    private WebSocket connection;

    public static ClientInfo of(RegisterInfo registerInfo, WebSocket connection) {
        ClientInfo clientInfo = new ClientInfo();

        clientInfo.clientId = Strings.nullToEmpty(registerInfo.getClientId());
        clientInfo.clientName = Strings.nullToEmpty(registerInfo.getClientName());
        clientInfo.connection = connection;

        return clientInfo;
    }
}
