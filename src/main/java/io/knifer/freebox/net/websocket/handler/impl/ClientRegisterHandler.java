package io.knifer.freebox.net.websocket.handler.impl;

import com.google.gson.reflect.TypeToken;
import io.knifer.freebox.constant.AppEvents;
import io.knifer.freebox.constant.I18nKeys;
import io.knifer.freebox.constant.MessageCodes;
import io.knifer.freebox.context.Context;
import io.knifer.freebox.helper.ToastHelper;
import io.knifer.freebox.model.c2s.RegisterInfo;
import io.knifer.freebox.model.common.tvbox.Message;
import io.knifer.freebox.model.domain.ClientInfo;
import io.knifer.freebox.net.websocket.core.ClientManager;
import io.knifer.freebox.net.websocket.exception.ForbiddenException;
import io.knifer.freebox.net.websocket.handler.KebSocketMessageHandler;
import io.knifer.freebox.util.json.GsonUtil;
import javafx.application.Platform;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.WebSocket;

/**
 * 客户端注册处理器
 *
 * @author Knifer
 */
@AllArgsConstructor
public class ClientRegisterHandler implements KebSocketMessageHandler<RegisterInfo> {

    private final ClientManager clientManager;

    @Override
    public boolean support(Message<?> message) {
        return MessageCodes.REGISTER == message.getCode();
    }

    @Override
    public Message<RegisterInfo> resolve(String messageString) {
        return GsonUtil.fromJson(messageString, new TypeToken<>(){});
    }

    @Override
    public void handle(Message<RegisterInfo> registerInfoMsg, WebSocket connection) {
        RegisterInfo registerInfo = registerInfoMsg.getData();
        ClientInfo clientInfo;

        if (registerInfo == null || StringUtils.isBlank(registerInfo.getClientId())) {
            throw new ForbiddenException(connection);
        } else {
            clientInfo = ClientInfo.of(registerInfo, connection);
            clientManager.register(clientInfo);
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
