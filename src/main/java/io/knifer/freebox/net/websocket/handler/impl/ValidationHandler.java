package io.knifer.freebox.net.websocket.handler.impl;

import io.knifer.freebox.constant.MessageCodes;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.net.websocket.exception.ForbiddenException;
import io.knifer.freebox.net.websocket.handler.KebSocketMessageHandler;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.WebSocket;

/**
 * 验证处理器
 *
 * @author Knifer
 */
@Slf4j
@AllArgsConstructor
public class ValidationHandler implements KebSocketMessageHandler {

    private final ClientManager clientManager;

    @Override
    public boolean support(Integer code) {
        return MessageCodes.REGISTER != code;
    }

    @Override
    public void handle(String messageData, WebSocket connection) {
        if (!clientManager.isRegistered(connection)) {
            log.warn("ip [{}] not registered", connection.getRemoteSocketAddress().getAddress().getHostAddress());

            throw new ForbiddenException(connection);
        }
    }
}
