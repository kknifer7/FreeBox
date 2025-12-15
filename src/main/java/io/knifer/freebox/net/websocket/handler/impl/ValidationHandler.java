package io.knifer.freebox.net.websocket.handler.impl;

import io.knifer.freebox.constant.MessageCodes;
import io.knifer.freebox.model.common.tvbox.Message;
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
public class ValidationHandler implements KebSocketMessageHandler<Void> {

    private final ClientManager clientManager;

    @Override
    public boolean support(Message<?> message) {
        return MessageCodes.REGISTER != message.getCode();
    }

    @Override
    public Message<Void> resolve(String messageString) {
        return null;
    }

    @Override
    public void handle(Message<Void> message, WebSocket connection) throws ForbiddenException {
        if (!clientManager.isRegistered(connection)) {
            log.warn("ip [{}] not registered", connection.getRemoteSocketAddress().getHostString());

            throw new ForbiddenException(connection);
        }
    }
}
