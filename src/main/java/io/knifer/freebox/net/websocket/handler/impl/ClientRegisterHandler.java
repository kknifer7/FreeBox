package io.knifer.freebox.net.websocket.handler.impl;

import com.google.gson.reflect.TypeToken;
import io.knifer.freebox.constant.*;
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
import org.apache.commons.lang3.math.NumberUtils;
import org.java_websocket.WebSocket;

/**
 * 客户端注册处理器
 *
 * @author Knifer
 */
@AllArgsConstructor
public class ClientRegisterHandler implements KebSocketMessageHandler<RegisterInfo> {

    private final ClientManager clientManager;

    private final static int SUPPORTED_KEB_SOCKET_PROTOCOL_VERSION_CODE;

    static {
        String code = BaseResources.X_PROPERTIES.getProperty(BaseValues.X_SUPPORTED_KEB_SOCKET_PROTOCOL_VERSION_CODE);

        SUPPORTED_KEB_SOCKET_PROTOCOL_VERSION_CODE = NumberUtils.toInt(code, 1);
    }

    @Override
    public boolean support(Message<?> message) {
        return MessageCodes.REGISTER == message.getCode();
    }

    @Override
    public Message<RegisterInfo> resolve(String messageString) {
        return GsonUtil.fromJson(messageString, new TypeToken<>(){});
    }

    @Override
    public void handle(Message<RegisterInfo> registerInfoMsg, WebSocket connection) throws ForbiddenException {
        RegisterInfo registerInfo = registerInfoMsg.getData();
        ClientInfo clientInfo;

        if (registerInfo == null || StringUtils.isBlank(registerInfo.getClientId())) {
            throw new ForbiddenException(connection);
        }
        if (isOldTVBoxApp(registerInfo)) {
            Platform.runLater(() -> ToastHelper.showInfoI18n(I18nKeys.MESSAGE_OLD_CLIENT_UNSUPPORTED));
            throw new ForbiddenException(connection);
        }
        clientInfo = ClientInfo.of(registerInfo, connection);
        clientManager.register(clientInfo);
        Platform.runLater(() -> {
            ToastHelper.showSuccessI18n(
                    I18nKeys.MESSAGE_CLIENT_REGISTERED,
                    clientInfo.getName()
            );
            Context.INSTANCE.postEvent(new AppEvents.ClientRegisteredEvent(clientInfo));
        });
    }

    private boolean isOldTVBoxApp(RegisterInfo registerInfo) {
        Integer protocolVersionCode = registerInfo.getProtocolVersionCode();

        return registerInfo.getKType() == null ||
                protocolVersionCode == null ||
                protocolVersionCode < SUPPORTED_KEB_SOCKET_PROTOCOL_VERSION_CODE;
    }
}
