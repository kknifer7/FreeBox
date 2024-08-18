package io.knifer.freebox.net.websocket.handler.impl;

import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.MessageCodes;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.c2s.RegisterInfo;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.net.websocket.exception.ForbiddenException;
import io.knifer.freebox.net.websocket.handler.KebSocketMessageHandler;
import io.knifer.freebox.util.GsonUtil;
import javafx.application.Platform;
import lombok.AllArgsConstructor;
import org.java_websocket.WebSocket;

/**
 * 客户端注册处理器
 *
 * @author Knifer
 */
@AllArgsConstructor
public class ClientRegisterHandler implements KebSocketMessageHandler {

    private final ClientManager clientManager;

    @Override
    public boolean support(Integer code) {
        return MessageCodes.REGISTER == code;
    }

    @Override
    public void handle(String messageData, WebSocket connection) {
        RegisterInfo registerInfo = GsonUtil.fromJson(messageData, RegisterInfo.class);
        ClientInfo clientInfo;

        if (registerInfo == null) {
            throw new ForbiddenException(connection);
        } else {
            clientInfo = ClientInfo.of(registerInfo, connection);
            clientManager.register(clientInfo, connection);
            Platform.runLater(() -> {
                ToastHelper.showSuccessI18n(
                        I18nKeys.MESSAGE_CLIENT_REGISTERED,
                        connection.getRemoteSocketAddress().getHostName()
                );
                Context.INSTANCE.postEvent(new AppEvents.ClientRegisteredEvent(clientInfo));
            });
        }
    }
}
